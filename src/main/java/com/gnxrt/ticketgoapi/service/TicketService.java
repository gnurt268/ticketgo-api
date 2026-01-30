package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.ticket.TransferTicketRequest;
import com.gnxrt.ticketgoapi.dto.response.ticket.TicketDTO;
import com.gnxrt.ticketgoapi.dto.response.ticket.TicketListDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ForbiddenException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.TicketRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final QRCodeService qrCodeService;
    private final EmailService emailService;

    public Page<TicketListDTO> getMyTickets(Pageable pageable) {
        User currentUser = getCurrentUser();
        log.info("Getting tickets for user: {}", currentUser.getEmail());

        Page<Ticket> tickets = ticketRepository.findByUserId(currentUser.getId(), pageable);
        return tickets.map(this::mapToListDTO);
    }

    public List<TicketListDTO> getMyUpcomingTickets() {
        User currentUser = getCurrentUser();
        log.info("Getting upcoming tickets for user: {}", currentUser.getEmail());

        Page<Ticket> allTickets = ticketRepository.findByUserId(currentUser.getId(), Pageable.unpaged());

        return allTickets.getContent().stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE)
                .filter(ticket -> ticket.getEvent().getStartDate().isAfter(LocalDateTime.now()))
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    public List<TicketListDTO> getMyPastTickets() {
        User currentUser = getCurrentUser();
        log.info("Getting past tickets for user: {}", currentUser.getEmail());

        Page<Ticket> allTickets = ticketRepository.findByUserId(currentUser.getId(), Pageable.unpaged());

        return allTickets.getContent().stream()
                .filter(ticket -> ticket.getEvent().getEndDate().isBefore(LocalDateTime.now()))
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    public TicketDTO getTicketDetail(Long ticketId) {
        User currentUser = getCurrentUser();
        log.info("Getting ticket detail: {} for user: {}", ticketId, currentUser.getEmail());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!ticket.getOrder().getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xem vé này");
        }

        return mapToDTO(ticket);
    }

    public TicketDTO getTicketByCode(String ticketCode) {
        User currentUser = getCurrentUser();
        log.info("Getting ticket by code: {} for user: {}", ticketCode, currentUser.getEmail());

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", ticketCode));

        if (!ticket.getOrder().getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xem vé này");
        }

        return mapToDTO(ticket);
    }

    public List<TicketDTO> getTicketsByOrder(Long orderId) {
        User currentUser = getCurrentUser();
        log.info("Getting tickets for order: {}", orderId);

        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);

        if (!tickets.isEmpty()) {
            Ticket firstTicket = tickets.get(0);
            if (!firstTicket.getOrder().getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
                throw new ForbiddenException("Bạn không có quyền xem các vé này");
            }
        }

        return tickets.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TicketDTO> getMyTicketsForEvent(Long eventId) {
        User currentUser = getCurrentUser();
        log.info("Getting tickets for event: {} for user: {}", eventId, currentUser.getEmail());

        List<Ticket> tickets = ticketRepository.findActiveTicketsByUserAndEvent(currentUser.getId(), eventId);

        return tickets.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketDTO transferTicket(Long ticketId, TransferTicketRequest request) {
        User currentUser = getCurrentUser();
        log.info("Transferring ticket: {} to: {}", ticketId, request.getRecipientEmail());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!ticket.getOrder().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không có quyền chuyển nhượng vé này");
        }

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể chuyển nhượng vé đang hoạt động");
        }

        if (ticket.getIsCheckedIn()) {
            throw new BadRequestException("Không thể chuyển nhượng vé đã check-in");
        }

        if (ticket.getEvent().getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Không thể chuyển nhượng vé cho sự kiện đã bắt đầu");
        }

        if (request.getRecipientEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new BadRequestException("Không thể chuyển nhượng vé cho chính mình");
        }

        String originalEmail = ticket.getHolderEmail();

        ticket.setHolderName(request.getRecipientName());
        ticket.setHolderEmail(request.getRecipientEmail());
        ticket.setHolderPhone(request.getRecipientPhone());
        ticket.setHolderIdNumber(request.getRecipientIdNumber());
        ticket.setTransferredFromEmail(originalEmail);
        ticket.setTransferredAt(LocalDateTime.now());
        ticket.setStatus(TicketStatus.TRANSFERRED);

        if (ticket.getFaceImageUrl() != null) {
            ticket.setFaceImageUrl(null);
            ticket.setFaceUploadedAt(null);
            ticket.setFaceEmbeddingId(null);
        }

        ticket.setQrCode(java.util.UUID.randomUUID().toString());

        ticket = ticketRepository.save(ticket);

        log.info("Ticket transferred: {} from {} to {}", ticketId, originalEmail, request.getRecipientEmail());

        emailService.sendTicketTransferEmail(ticket, originalEmail);

        ticket.setStatus(TicketStatus.ACTIVE);
        ticket = ticketRepository.save(ticket);

        return mapToDTO(ticket);
    }

    public byte[] downloadTicketQRCode(Long ticketId) {
        User currentUser = getCurrentUser();
        log.info("Downloading QR code for ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!ticket.getOrder().getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền tải QR code này");
        }

        String content = ticket.getTicketCode() + "|" + ticket.getQrCode();
        return qrCodeService.generateQRCodeBytes(content);
    }

    public TicketDTO validateTicketForCheckIn(String ticketCode) {
        log.info("Validating ticket for check-in: {}", ticketCode);

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", ticketCode));

        return mapToDTO(ticket);
    }

    public TicketDTO validateTicketByQRCode(String qrContent) {
        log.info("Validating ticket by QR code");

        QRCodeService.QRCodeValidationResult result = qrCodeService.validateQRCode(qrContent);

        if (!result.isValid()) {
            throw new BadRequestException(result.getErrorMessage());
        }

        Ticket ticket = ticketRepository.findByTicketCode(result.getTicketCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", result.getTicketCode()));

        if (!ticket.getQrCode().equals(result.getQrCode())) {
            throw new BadRequestException("QR code không hợp lệ");
        }

        return mapToDTO(ticket);
    }

    public Page<TicketListDTO> getTicketsByEvent(Long eventId, Pageable pageable) {
        log.info("Getting tickets for event: {}", eventId);

        Page<Ticket> tickets = ticketRepository.findByEventIdOrderByCreatedAtDesc(eventId, pageable);
        return tickets.map(this::mapToListDTO);
    }

    public Page<TicketListDTO> getTicketsByEventAndStatus(Long eventId, TicketStatus status, Pageable pageable) {
        log.info("Getting tickets for event: {} with status: {}", eventId, status);

        Page<Ticket> tickets = ticketRepository.findByEventIdAndStatusOrderByCreatedAtDesc(eventId, status, pageable);
        return tickets.map(this::mapToListDTO);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private TicketDTO mapToDTO(Ticket ticket) {
        Event event = ticket.getEvent();
        LocalDateTime now = LocalDateTime.now();

        boolean isValid = ticket.getStatus() == TicketStatus.ACTIVE && !ticket.getIsCheckedIn();
        boolean canCheckIn = isValid &&
                event.getStatus() == EventStatus.PUBLISHED &&
                event.getStartDate().minusHours(2).isBefore(now) &&
                event.getEndDate().isAfter(now);
        boolean canTransfer = ticket.getStatus() == TicketStatus.ACTIVE &&
                !ticket.getIsCheckedIn() &&
                event.getStartDate().isAfter(now);

        String qrCodeUrl = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());

        String statusMessage = getStatusMessage(ticket, event);

        return TicketDTO.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .qrCode(ticket.getQrCode())
                .qrCodeUrl(qrCodeUrl)
                // Event info
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventSlug(event.getSlug())
                .eventPosterUrl(event.getPosterUrl())
                .eventBannerUrl(event.getBannerUrl())
                .eventStartDate(event.getStartDate())
                .eventEndDate(event.getEndDate())
                .eventVenue(event.getVenue())
                .eventAddress(event.getAddress())
                .eventCity(event.getCity())
                // Zone info
                .ticketZoneId(ticket.getTicketZone().getId())
                .zoneName(ticket.getTicketZone().getZoneName())
                .zoneCode(ticket.getTicketZone().getZoneCode())
                .zoneColorCode(ticket.getTicketZone().getColorCode())
                .zonePrice(ticket.getTicketZone().getPrice())
                // Seat info
                .seatId(ticket.getSeat() != null ? ticket.getSeat().getId() : null)
                .seatCode(ticket.getSeatNumber())
                .rowNumber(ticket.getRowNumber())
                .seatNumber(ticket.getSeatNumber())
                // Holder info
                .holderName(ticket.getHolderName())
                .holderEmail(ticket.getHolderEmail())
                .holderPhone(ticket.getHolderPhone())
                .holderIdNumber(ticket.getHolderIdNumber())
                // Order info
                .orderId(ticket.getOrder().getId())
                .orderCode(ticket.getOrder().getOrderCode())
                // Status
                .status(ticket.getStatus())
                .isCheckedIn(ticket.getIsCheckedIn())
                .checkedInAt(ticket.getCheckedInAt())
                .checkedInBy(ticket.getCheckedInBy())
                .checkedInConfidence(ticket.getCheckedInConfidence())
                // Face recognition
                .hasFaceImage(ticket.getFaceImageUrl() != null)
                .faceImageUrl(ticket.getFaceImageUrl())
                .faceUploadedAt(ticket.getFaceUploadedAt())
                .requiresFaceUpload(event.getRequireFaceUpload() && ticket.getFaceImageUrl() == null)
                // Transfer info
                .transferredFromEmail(ticket.getTransferredFromEmail())
                .transferredAt(ticket.getTransferredAt())
                // Timestamps
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                // Computed
                .isValid(isValid)
                .canCheckIn(canCheckIn)
                .canTransfer(canTransfer)
                .statusMessage(statusMessage)
                .build();
    }

    private TicketListDTO mapToListDTO(Ticket ticket) {
        Event event = ticket.getEvent();
        LocalDateTime now = LocalDateTime.now();

        boolean isUpcoming = event.getStartDate().isAfter(now);
        boolean isPast = event.getEndDate().isBefore(now);
        boolean isValid = ticket.getStatus() == TicketStatus.ACTIVE && !ticket.getIsCheckedIn();

        String qrCodeUrl = qrCodeService.generateTicketQRCodeBase64(ticket.getTicketCode(), ticket.getQrCode());

        return TicketListDTO.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .qrCodeUrl(qrCodeUrl)
                // Event info
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventSlug(event.getSlug())
                .eventPosterUrl(event.getPosterUrl())
                .eventStartDate(event.getStartDate())
                .eventVenue(event.getVenue())
                .eventCity(event.getCity())
                // Zone info
                .zoneName(ticket.getTicketZone().getZoneName())
                .zoneColorCode(ticket.getTicketZone().getColorCode())
                // Seat info
                .seatCode(ticket.getSeatNumber())
                // Holder info
                .holderName(ticket.getHolderName())
                // Status
                .status(ticket.getStatus())
                .isCheckedIn(ticket.getIsCheckedIn())
                // Computed
                .isUpcoming(isUpcoming)
                .isPast(isPast)
                .isValid(isValid)
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    private String getStatusMessage(Ticket ticket, Event event) {
        LocalDateTime now = LocalDateTime.now();

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return "Vé đã bị hủy";
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            return "Vé đã được hoàn tiền";
        }
        if (ticket.getIsCheckedIn()) {
            return "Đã check-in lúc " + ticket.getCheckedInAt();
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            return "Sự kiện đã bị hủy";
        }
        if (event.getEndDate().isBefore(now)) {
            return "Sự kiện đã kết thúc";
        }
        if (event.getStartDate().isAfter(now)) {
            return "Chờ đến ngày sự kiện";
        }
        if (event.getStartDate().isBefore(now) && event.getEndDate().isAfter(now)) {
            return "Sự kiện đang diễn ra - Có thể check-in";
        }

        return "Vé hợp lệ";
    }
}