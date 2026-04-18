package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.order.CreateOrderRequest;
import com.gnxrt.ticketgoapi.dto.response.order.OrderDTO;
import com.gnxrt.ticketgoapi.dto.response.payment.PaymentDTO;
import com.gnxrt.ticketgoapi.dto.response.payment.VNPayCallbackDTO;
import com.gnxrt.ticketgoapi.enums.*;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ForbiddenException;
import com.gnxrt.ticketgoapi.exception.PaymentException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.*;
import com.gnxrt.ticketgoapi.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;
    private final TicketZoneRepository ticketZoneRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final VNPayService vnPayService;
    private final DistributedLockService distributedLockService;
    private final EmailService emailService;

    private static final int PAYMENT_TIMEOUT_MINUTES = 15;
    private static final long LOCK_WAIT_TIME = 10; // seconds
    private static final long LOCK_LEASE_TIME = 60; // seconds

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, HttpServletRequest httpRequest) {
        log.info("Creating order for event: {}, zone: {}", request.getEventId(), request.getTicketZoneId());

        // Check if this is a seat-based booking
        boolean hasSeatSelection = request.getSeatIds() != null && !request.getSeatIds().isEmpty();

        if (hasSeatSelection) {
            List<Long> seatIds = request.getSeatIds();
            boolean locked = distributedLockService.tryLockSeats(seatIds, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new ConflictException("Ghế đang được người khác đặt. Vui lòng thử lại sau vài giây.");
            }

            try {
                return doCreateOrder(request, httpRequest);
            } finally {
                distributedLockService.unlockSeats(seatIds);
            }
        } else {
            Long zoneId = request.getTicketZoneId();
            boolean locked = distributedLockService.tryLockZone(zoneId, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new ConflictException("Khu vực đang bận. Vui lòng thử lại sau vài giây.");
            }

            try {
                return doCreateOrder(request, httpRequest);
            } finally {
                distributedLockService.unlockZone(zoneId);
            }
        }
    }

    private OrderDTO doCreateOrder(CreateOrderRequest request, HttpServletRequest httpRequest) {
        User currentUser = getCurrentUser();

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Sự kiện không khả dụng để đặt vé");
        }

        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Sự kiện đã bắt đầu");
        }

        TicketZone zone = ticketZoneRepository.findById(request.getTicketZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("TicketZone", "id", request.getTicketZoneId()));

        if (!zone.getEvent().getId().equals(event.getId())) {
            throw new BadRequestException("Khu vực vé không thuộc sự kiện này");
        }

        if (!zone.getIsActive() || !zone.isSaleActive()) {
            throw new BadRequestException("Khu vực vé không khả dụng để bán");
        }

        List<Seat> seats = new ArrayList<>();
        int quantity;

        if (event.getEnableSeatSelection() && request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            seats = seatRepository.findByIdIn(request.getSeatIds());

            if (seats.size() != request.getSeatIds().size()) {
                throw new ResourceNotFoundException("Một số ghế không tìm thấy");
            }

            for (Seat seat : seats) {
                if (!seat.getTicketZone().getId().equals(zone.getId())) {
                    throw new BadRequestException("Ghế " + seat.getSeatCode() + " không thuộc khu vực này");
                }

                boolean isAvailable = seat.getStatus() == SeatStatus.AVAILABLE;
                boolean isReservedByCurrentUser = seat.getStatus() == SeatStatus.RESERVED
                        && seat.getReservedBy() != null
                        && seat.getReservedBy().getId().equals(currentUser.getId());

                if (!isAvailable && !isReservedByCurrentUser) {
                    throw new ConflictException("Ghế " + seat.getSeatCode() + " không khả dụng hoặc đã được đặt bởi người khác");
                }
            }

            quantity = seats.size();
        } else {
            quantity = request.getQuantity() != null ? request.getQuantity() : 1;

            if (zone.getAvailableCapacity() < quantity) {
                throw new ConflictException("Không đủ vé. Còn lại: " + zone.getAvailableCapacity());
            }
        }

        if (quantity > event.getMaxTicketsPerOrder()) {
            throw new BadRequestException("Không thể đặt quá " + event.getMaxTicketsPerOrder() + " vé mỗi đơn hàng");
        }

        List<CreateOrderRequest.AttendeeInfo> attendees = request.getAttendees();
        if (attendees == null || attendees.isEmpty()) {
            attendees = new ArrayList<>();
            for (int i = 0; i < quantity; i++) {
                attendees.add(CreateOrderRequest.AttendeeInfo.builder()
                        .name(request.getBuyerName())
                        .email(request.getBuyerEmail())
                        .phone(request.getBuyerPhone())
                        .build());
            }
        } else if (attendees.size() != quantity) {
            throw new BadRequestException("Số lượng người tham dự phải bằng số lượng vé");
        }

        BigDecimal unitPrice = zone.getPrice();
        BigDecimal totalAmount;

        if (!seats.isEmpty()) {
            totalAmount = seats.stream()
                    .map(Seat::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        }

        String orderCode = generateOrderCode();

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(currentUser)
                .event(event)
                .totalAmount(totalAmount)
                .currency(zone.getCurrency())
                .quantity(quantity)
                .paymentStatus(PaymentStatus.PENDING)
                .buyerName(request.getBuyerName())
                .buyerEmail(request.getBuyerEmail())
                .buyerPhone(request.getBuyerPhone())
                .notes(request.getNotes())
                .build();

        order = orderRepository.save(order);

        String txnRef = buildVnpTxnRef(order.getOrderCode(), order.getId());

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .amount(totalAmount)
                .vnpTxnRef(txnRef)
                .ipAddress(vnPayService.getIpAddress(httpRequest))
                .build();
        paymentRepository.save(payment);

        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            CreateOrderRequest.AttendeeInfo attendee = attendees.get(i);
            Seat seat = seats.isEmpty() ? null : seats.get(i);

            Ticket ticket = Ticket.builder()
                    .ticketCode(generateTicketCode())
                    .order(order)
                    .event(event)
                    .ticketZone(zone)
                    .seat(seat)
                    .holderName(attendee.getName())
                    .holderEmail(attendee.getEmail())
                    .holderPhone(attendee.getPhone())
                    .holderIdNumber(attendee.getIdNumber())
                    .seatNumber(seat != null ? seat.getSeatCode() : null)
                    .rowNumber(seat != null ? seat.getRowLabel() : null)
                    .qrCode(generateQRCode())
                    .status(TicketStatus.PENDING)
                    .build();

            tickets.add(ticket);
        }

        ticketRepository.saveAll(tickets);

        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                seat.setStatus(SeatStatus.RESERVED);
                seat.setReservedBy(currentUser);
                seat.setReservedUntil(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES));
            }
        }
        if (!seats.isEmpty()) {
            seatRepository.saveAll(seats);
        }

        if (seats.isEmpty()) {
            zone.setAvailableCapacity(zone.getAvailableCapacity() - quantity);
            zone.setReservedCapacity(zone.getReservedCapacity() + quantity);
            ticketZoneRepository.save(zone);
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                txnRef,
                totalAmount,
                "Thanh toan ve su kien: " + event.getTitle(),
                vnPayService.getIpAddress(httpRequest)
        );

        log.info("Order created: {}, amount: {}", orderCode, totalAmount);

        return mapToDTO(order, tickets, paymentUrl);
    }

    @Transactional
    public OrderDTO processPaymentCallback(HttpServletRequest request) {
        log.info("Processing payment callback");

        if (!vnPayService.validateSignature(request)) {
            throw new PaymentException("Chữ ký thanh toán không hợp lệ");
        }

        VNPayCallbackDTO callback = vnPayService.processCallback(request);
        String vnpTxnRef = callback.getVnpTxnRef();

        Payment payment = paymentRepository.findByVnpTxnRef(vnpTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "vnpTxnRef", vnpTxnRef));

        OrderDTO dto = applyCallbackResult(payment, callback);

        if (payment.getStatus() == PaymentStatus.FAILED && !callback.isSuccess()) {
            throw new PaymentException(callback.getResponseMessage());
        }

        return dto;
    }

    /**
     * Entry-point cho reconciliation job: re-fetch Payment trong tx rồi apply.
     * Dùng khi caller có Payment từ query ngoài tx (lazy associations chưa load).
     */
    @Transactional
    public void applyQuerydrResult(Long paymentId, VNPayCallbackDTO callback) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        applyCallbackResult(payment, callback);
    }

    /**
     * Áp kết quả callback/querydr của VNPay lên Payment + Order + Tickets.
     * Dùng chung cho cả return URL/IPN callback và job reconciliation.
     *
     * Caller phải đảm bảo Payment còn managed (gọi trong tx).
     * Idempotent: nếu Payment không còn PENDING thì no-op và trả DTO hiện tại.
     * Không throw khi payment fail — caller tự quyết định hành vi.
     */
    @Transactional
    public OrderDTO applyCallbackResult(Payment payment, VNPayCallbackDTO callback) {
        Order order = payment.getOrder();
        String vnpTxnRef = payment.getVnpTxnRef();
        String orderCode = order.getOrderCode();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment already processed: vnpTxnRef={}, status={}", vnpTxnRef, payment.getStatus());
            List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
            return mapToDTO(order, tickets, null);
        }

        if (callback.isSuccess()) {
            log.info("Payment successful for order: {}", orderCode);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(callback.getVnpTransactionNo());
            payment.setBankCode(callback.getVnpBankCode());
            payment.setBankTransactionNo(callback.getVnpBankTranNo());
            payment.setCardType(callback.getVnpCardType());
            payment.setResponseCode(callback.getVnpResponseCode());
            payment.setResponseMessage(callback.getResponseMessage());
            payment.setPaidAt(LocalDateTime.ofInstant(
                    vnPayService.parsePayDate(callback.getVnpPayDate()).toInstant(),
                    ZoneId.systemDefault()
            ));
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.COMPLETED);

            List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
            for (Ticket ticket : tickets) {
                ticket.setStatus(TicketStatus.ACTIVE);

                if (ticket.getSeat() != null) {
                    Seat seat = ticket.getSeat();
                    seat.setStatus(SeatStatus.SOLD);
                    seat.setReservedBy(null);
                    seat.setReservedUntil(null);
                    seatRepository.save(seat);
                }
            }
            ticketRepository.saveAll(tickets);

            TicketZone zone = tickets.getFirst().getTicketZone();
            zone.setReservedCapacity(zone.getReservedCapacity() - order.getQuantity());
            ticketZoneRepository.save(zone);

            Event event = order.getEvent();
            event.setTotalTicketsSold(event.getTotalTicketsSold() + order.getQuantity());
            event.setTotalRevenue(event.getTotalRevenue().add(order.getTotalAmount()));
            eventRepository.save(event);

            orderRepository.save(order);

            emailService.sendOrderConfirmationEmail(order, tickets);

            return mapToDTO(order, tickets, null);
        }

        // Order ở trạng thái PENDING để user có thể retry (tạo Payment mới).
        // Tickets/seats/capacity chỉ được release khi user cancel hoặc job cancelExpiredOrders chạy.
        log.warn("Payment failed for order: {}, code: {}", orderCode, callback.getVnpResponseCode());

        payment.setStatus(PaymentStatus.FAILED);
        payment.setResponseCode(callback.getVnpResponseCode());
        payment.setResponseMessage(callback.getResponseMessage());
        payment.setBankCode(callback.getVnpBankCode());
        payment.setBankTransactionNo(callback.getVnpBankTranNo());
        payment.setCardType(callback.getVnpCardType());
        paymentRepository.save(payment);

        orderRepository.save(order);

        emailService.sendPaymentFailedEmail(order, callback.getResponseMessage());

        List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
        return mapToDTO(order, tickets, null);
    }

    public OrderDTO getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xem đơn hàng này");
        }

        List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
        return mapToDTO(order, tickets, null);
    }

    @Transactional
    public PaymentDTO retryPayment(String orderCode, HttpServletRequest httpRequest) {
        log.info("Retrying payment for order: {}", orderCode);

        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền thanh toán đơn hàng này");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể retry đơn hàng đang chờ thanh toán");
        }

        LocalDateTime expiredAt = order.getCreatedAt().plusMinutes(PAYMENT_TIMEOUT_MINUTES);
        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiredAt);
        if (remainingSeconds <= 0) {
            throw new BadRequestException("Đơn hàng đã hết hạn. Vui lòng tạo đơn mới.");
        }

        paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.PENDING)
                .ifPresent(p -> {
                    p.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(p);
                });

        String ipAddress = vnPayService.getIpAddress(httpRequest);
        String txnRef = buildVnpTxnRef(order.getOrderCode(), order.getId());

        Payment newPayment = Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .vnpTxnRef(txnRef)
                .ipAddress(ipAddress)
                .build();
        paymentRepository.save(newPayment);

        String paymentUrl = vnPayService.createPaymentUrl(
                txnRef,
                order.getTotalAmount(),
                "Thanh toan ve su kien: " + order.getEvent().getTitle(),
                ipAddress
        );

        return PaymentDTO.builder()
                .orderCode(order.getOrderCode())
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentUrl(paymentUrl)
                .expiredAt(expiredAt)
                .remainingSeconds((int) remainingSeconds)
                .message("Payment URL generated successfully")
                .build();
    }

    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xem đơn hàng này");
        }

        List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
        return mapToDTO(order, tickets, null);
    }

    public Page<OrderDTO> getUserOrders(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        return orders.map(order -> {
            List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
            return mapToDTO(order, tickets, null);
        });
    }

    public Page<OrderDTO> getAllOrdersForAdmin(String keyword, PaymentStatus paymentStatus, Pageable pageable) {
        Page<Order> orders = orderRepository.findAllForAdmin(keyword, paymentStatus, pageable);

        return orders.map(order -> {
            List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
            return mapToDTO(order, tickets, null);
        });
    }

    public OrderDTO getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
        return mapToDTO(order, tickets, null);
    }

    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByPaymentStatus(PaymentStatus.PENDING));
        stats.put("completedOrders", orderRepository.countByPaymentStatus(PaymentStatus.COMPLETED));
        stats.put("failedOrders", orderRepository.countByPaymentStatus(PaymentStatus.FAILED));
        stats.put("cancelledOrders", orderRepository.countByPaymentStatus(PaymentStatus.CANCELLED));
        stats.put("totalRevenue", orderRepository.getTotalRevenue());
        stats.put("averageOrderValue", orderRepository.getAverageOrderValue());

        return stats;
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        log.info("Cancelling order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        User currentUser = getCurrentUser();
        if (!order.getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng đang chờ thanh toán");
        }

        order.setPaymentStatus(PaymentStatus.CANCELLED);

        paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.PENDING)
                .ifPresent(p -> {
                    p.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(p);
                });

        List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);

            if (ticket.getSeat() != null) {
                Seat seat = ticket.getSeat();
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setReservedBy(null);
                seat.setReservedUntil(null);
                seatRepository.save(seat);
            }
        }
        ticketRepository.saveAll(tickets);

        TicketZone zone = tickets.get(0).getTicketZone();
        zone.setAvailableCapacity(zone.getAvailableCapacity() + order.getQuantity());
        zone.setReservedCapacity(zone.getReservedCapacity() - order.getQuantity());
        ticketZoneRepository.save(zone);

        orderRepository.save(order);

        log.info("Order cancelled: {}", orderId);
        return mapToDTO(order, tickets, null);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Order> expiredOrders = orderRepository.findByPaymentStatusAndCreatedAtBefore(
                PaymentStatus.PENDING, expiredTime
        );

        if (!expiredOrders.isEmpty()) {
            log.info("Cancelling {} expired orders", expiredOrders.size());

            for (Order order : expiredOrders) {
                try {
                    order.setPaymentStatus(PaymentStatus.EXPIRED);

                    paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.PENDING)
                            .ifPresent(p -> {
                                p.setStatus(PaymentStatus.EXPIRED);
                                paymentRepository.save(p);
                            });

                    List<Ticket> tickets = ticketRepository.findByOrderId(order.getId());
                    for (Ticket ticket : tickets) {
                        ticket.setStatus(TicketStatus.CANCELLED);

                        if (ticket.getSeat() != null) {
                            Seat seat = ticket.getSeat();
                            seat.setStatus(SeatStatus.AVAILABLE);
                            seat.setReservedBy(null);
                            seat.setReservedUntil(null);
                            seatRepository.save(seat);
                        }
                    }
                    ticketRepository.saveAll(tickets);

                    if (!tickets.isEmpty()) {
                        TicketZone zone = tickets.get(0).getTicketZone();
                        zone.setAvailableCapacity(zone.getAvailableCapacity() + order.getQuantity());
                        zone.setReservedCapacity(Math.max(0, zone.getReservedCapacity() - order.getQuantity()));
                        ticketZoneRepository.save(zone);
                    }

                    orderRepository.save(order);
                    log.info("Expired order cancelled: {}", order.getOrderCode());
                } catch (Exception e) {
                    log.error("Error cancelling expired order: {}", order.getOrderCode(), e);
                }
            }
        }
    }

    private String generateOrderCode() {
        return "TG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String buildVnpTxnRef(String orderCode, Long orderId) {
        long attempt = paymentRepository.countByOrderId(orderId) + 1;
        return orderCode + "-" + attempt;
    }

    private String generateTicketCode() {
        return "TK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateQRCode() {
        return UUID.randomUUID().toString();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private OrderDTO mapToDTO(Order order, List<Ticket> tickets, String paymentUrl) {
        Event event = order.getEvent();

        LocalDateTime paymentExpiredAt = order.getCreatedAt().plusMinutes(PAYMENT_TIMEOUT_MINUTES);
        int remainingSeconds = (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), paymentExpiredAt);

        List<OrderDTO.TicketSummary> ticketSummaries = tickets.stream()
                .map(ticket -> OrderDTO.TicketSummary.builder()
                        .id(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .holderName(ticket.getHolderName())
                        .holderEmail(ticket.getHolderEmail())
                        .seatCode(ticket.getSeatNumber())
                        .rowNumber(ticket.getRowNumber())
                        .status(ticket.getStatus().name())
                        .isCheckedIn(ticket.getIsCheckedIn())
                        .build())
                .collect(Collectors.toList());

        TicketZone zone = tickets.isEmpty() ? null : tickets.get(0).getTicketZone();

        Payment payment = order.getSuccessfulPayment();
        if (payment == null) {
            payment = order.getLatestPayment();
        }

        return OrderDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventSlug(event.getSlug())
                .eventPosterUrl(event.getPosterUrl())
                .eventStartDate(event.getStartDate())
                .eventVenue(event.getVenue())
                .eventAddress(event.getAddress())
                .ticketZoneId(zone != null ? zone.getId() : null)
                .zoneName(zone != null ? zone.getZoneName() : null)
                .zoneCode(zone != null ? zone.getZoneCode() : null)
                .quantity(order.getQuantity())
                .unitPrice(zone != null ? zone.getPrice() : null)
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .paymentStatus(order.getPaymentStatus())
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paidAt(payment != null ? payment.getPaidAt() : null)
                .buyerName(order.getBuyerName())
                .buyerEmail(order.getBuyerEmail())
                .buyerPhone(order.getBuyerPhone())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName())
                .notes(order.getNotes())
                .tickets(ticketSummaries)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paymentUrl(paymentUrl)
                .paymentExpiredAt(paymentExpiredAt)
                .remainingSeconds(Math.max(0, remainingSeconds))
                .build();
    }
}