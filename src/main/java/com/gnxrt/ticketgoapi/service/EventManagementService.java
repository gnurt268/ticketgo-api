package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.CacheConfig;
import com.gnxrt.ticketgoapi.dto.request.event.EventApprovalRequest;
import com.gnxrt.ticketgoapi.dto.request.event.EventRequest;
import com.gnxrt.ticketgoapi.dto.response.event.EventDetailDTO;
import com.gnxrt.ticketgoapi.dto.response.event.EventListDTO;
import com.gnxrt.ticketgoapi.dto.response.organizer.OrganizerDashboardDTO;
import com.gnxrt.ticketgoapi.dto.response.organizer.RevenueStatisticsDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ForbiddenException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Category;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Order;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.CategoryRepository;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.OrderRepository;
import com.gnxrt.ticketgoapi.repository.TicketRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventManagementService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final RefundService refundService;

    public Page<EventListDTO> getAllEvents(
            EventStatus status,
            Long categoryId,
            Long organizerId,
            EventType eventType,
            String city,
            Boolean isFeatured,
            String keyword,
            Pageable pageable
    ) {
        log.info("Getting all events with filters");

        Page<Event> events;

        if (keyword != null && !keyword.trim().isEmpty()) {
            events = eventRepository.searchAllEvents(keyword, pageable);
        } else {
            events = eventRepository.findByFilters(
                    status, categoryId, organizerId, eventType, city, isFeatured,
                    null,
                    pageable
            );
        }

        return events.map(this::mapToListDTO);
    }

    public Page<EventListDTO> getMyEvents(EventStatus status, String keyword, Pageable pageable) {
        User organizer = getCurrentUser();
        log.info("Getting events of organizer id: {}", organizer.getId());

        Page<Event> events = eventRepository.findByFilters(
                status, null, organizer.getId(), null, null, null, null, pageable
        );

        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowered = keyword.toLowerCase();
            List<Event> filtered = events.getContent().stream()
                    .filter(e -> (e.getTitle() != null && e.getTitle().toLowerCase().contains(lowered))
                            || (e.getVenue() != null && e.getVenue().toLowerCase().contains(lowered))
                            || (e.getCity() != null && e.getCity().toLowerCase().contains(lowered)))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(
                    filtered.stream().map(this::mapToListDTO).toList(),
                    pageable,
                    filtered.size()
            );
        }

        return events.map(this::mapToListDTO);
    }

    public Map<String, Object> getMyEventStatistics() {
        User organizer = getCurrentUser();
        Long organizerId = organizer.getId();
        log.info("Getting statistics for organizer id: {}", organizerId);

        Map<String, Object> stats = new HashMap<>();

        long total = 0L;
        int totalTicketsSold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, Long> byStatus = new HashMap<>();

        for (EventStatus s : EventStatus.values()) {
            List<Event> events = eventRepository.findByOrganizerIdAndStatusIn(organizerId, List.of(s));
            byStatus.put(s.name(), (long) events.size());
            total += events.size();
            for (Event e : events) {
                if (e.getTotalTicketsSold() != null) {
                    totalTicketsSold += e.getTotalTicketsSold();
                }
                if (e.getTotalRevenue() != null) {
                    totalRevenue = totalRevenue.add(e.getTotalRevenue());
                }
            }
        }

        stats.put("totalEvents", total);
        stats.put("byStatus", byStatus);
        stats.put("totalTicketsSold", totalTicketsSold);
        stats.put("totalRevenue", totalRevenue);

        return stats;
    }

    @Transactional(readOnly = true)
    public RevenueStatisticsDTO getRevenueStatistics(String period) {
        int days = parsePeriodDays(period);
        User currentUser = getCurrentUser();
        Long organizerId = currentUser.isAdmin() ? null : currentUser.getId();

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zone);
        LocalDate fromDate = today.minusDays(days - 1L);
        LocalDateTime fromTs = fromDate.atStartOfDay();
        LocalDateTime toTs = today.plusDays(1).atStartOfDay();

        List<Object[]> rows = orderRepository.findDailyRevenueStats(organizerId, fromTs, toTs);

        Map<String, Object[]> byDate = rows.stream()
                .collect(Collectors.toMap(r -> r[0].toString(), r -> r, (a, b) -> a));

        List<RevenueStatisticsDTO.DailyPoint> series = new ArrayList<>(days);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalTicketsSold = 0L;

        for (int i = 0; i < days; i++) {
            LocalDate d = fromDate.plusDays(i);
            String key = d.toString();
            Object[] row = byDate.get(key);
            BigDecimal revenue = row != null ? toBigDecimal(row[1]) : BigDecimal.ZERO;
            long tickets = row != null ? ((Number) row[2]).longValue() : 0L;

            series.add(RevenueStatisticsDTO.DailyPoint.builder()
                    .date(key)
                    .revenue(revenue)
                    .ticketsSold(tickets)
                    .build());

            totalRevenue = totalRevenue.add(revenue);
            totalTicketsSold += tickets;
        }

        return RevenueStatisticsDTO.builder()
                .period(period)
                .from(fromDate.toString())
                .to(today.toString())
                .totalRevenue(totalRevenue)
                .totalTicketsSold(totalTicketsSold)
                .series(series)
                .build();
    }

    private int parsePeriodDays(String period) {
        if (period == null) {
            return 7;
        }
        return switch (period.trim().toLowerCase()) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new BadRequestException("period phải là 7d, 30d hoặc 90d");
        };
    }

    @Transactional(readOnly = true)
    public OrganizerDashboardDTO getOrganizerDashboard() {
        User organizer = getCurrentUser();
        Long organizerId = organizer.getId();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        long totalEvents = eventRepository.countByOrganizerId(organizerId);
        long totalTicketsSold = ticketRepository.countActiveByOrganizerId(organizerId);
        BigDecimal totalRevenue = orderRepository.sumRevenueByOrganizerId(organizerId);
        long totalCheckIns = ticketRepository.countCheckedInByOrganizerId(organizerId);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (EventStatus s : EventStatus.values()) byStatus.put(s.name(), 0L);
        eventRepository.countByOrganizerIdGroupByStatus(organizerId)
            .forEach(row -> byStatus.put(((EventStatus) row[0]).name(), ((Number) row[1]).longValue()));

        List<OrganizerDashboardDTO.RevenueByDay> revenueChart =
            orderRepository.findRevenueByDayForOrganizer(organizerId, thirtyDaysAgo)
                .stream()
                .map(row -> OrganizerDashboardDTO.RevenueByDay.builder()
                    .date(row[0].toString())
                    .revenue(toBigDecimal(row[1]))
                    .orderCount(((Number) row[2]).longValue())
                    .build())
                .collect(Collectors.toList());

        List<OrganizerDashboardDTO.TicketSalesByDay> ticketSalesChart =
            ticketRepository.findTicketSalesByDayForOrganizer(organizerId, thirtyDaysAgo)
                .stream()
                .map(row -> OrganizerDashboardDTO.TicketSalesByDay.builder()
                    .date(row[0].toString())
                    .ticketsSold(((Number) row[1]).longValue())
                    .build())
                .collect(Collectors.toList());

        List<Object[]> topEventsRaw = orderRepository.findTopEventsByRevenueForOrganizer(
                organizerId, PageRequest.of(0, 5));
        List<Long> topEventIds = topEventsRaw.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());
        Map<Long, Long> ticketCountByEvent = topEventIds.isEmpty()
                ? Map.of()
                : ticketRepository.countActiveTicketsByEventIds(topEventIds).stream()
                    .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()));

        List<OrganizerDashboardDTO.TopEventDTO> topEvents = topEventsRaw.stream()
            .map(row -> {
                Long eventId = ((Number) row[0]).longValue();
                return OrganizerDashboardDTO.TopEventDTO.builder()
                    .eventId(eventId)
                    .eventTitle((String) row[1])
                    .revenue(toBigDecimal(row[2]))
                    .ticketsSold(ticketCountByEvent.getOrDefault(eventId, 0L))
                    .build();
            })
            .collect(Collectors.toList());

        List<OrganizerDashboardDTO.CheckInRateDTO> checkInRates =
            ticketRepository.findCheckInStatsByOrganizer(organizerId)
                .stream()
                .map(row -> {
                    long total = ((Number) row[2]).longValue();
                    long checked = ((Number) row[3]).longValue();
                    return OrganizerDashboardDTO.CheckInRateDTO.builder()
                        .eventId(((Number) row[0]).longValue())
                        .eventTitle((String) row[1])
                        .totalTickets(total)
                        .checkedIn(checked)
                        .checkInRate(total > 0 ? (double) checked / total : 0.0)
                        .build();
                })
                .collect(Collectors.toList());

        return OrganizerDashboardDTO.builder()
            .totalEvents(totalEvents)
            .totalTicketsSold(totalTicketsSold)
            .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
            .totalCheckIns(totalCheckIns)
            .eventsByStatus(byStatus)
            .revenueChart(revenueChart)
            .ticketSalesChart(ticketSalesChart)
            .topEventsByRevenue(topEvents)
            .checkInRates(checkInRates)
            .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    public Page<EventListDTO> getPendingEvents(Pageable pageable) {
        log.info("Getting pending events for approval");
        return eventRepository.findPendingEvents(pageable)
                .map(this::mapToListDTO);
    }

    public Long countPendingEvents() {
        return eventRepository.countPendingEvents();
    }

    public EventDetailDTO getEventDetail(Long eventId) {
        log.info("Getting event detail for id: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));
        return mapToDetailDTO(event);
    }

    @Transactional
    public EventDetailDTO createEvent(EventRequest request) {
        log.info("Creating new event: {}", request.getTitle());

        if (eventRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Slug sự kiện đã tồn tại: " + request.getSlug());
        }

        User organizer = getCurrentUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Event event = Event.builder()
                .organizer(organizer)
                .category(category)
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .posterUrl(request.getPosterUrl())
                .bannerUrl(request.getBannerUrl())
                .location(request.getLocation())
                .venue(request.getVenue())
                .address(request.getAddress())
                .city(request.getCity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(EventStatus.DRAFT)
                .eventType(request.getEventType())
                .isFeatured(false)
                .maxTicketsPerOrder(request.getMaxTicketsPerOrder())
                .maxTicketsPerUser(request.getMaxTicketsPerUser())
                .enableSeatSelection(request.getEnableSeatSelection())
                .seatMapImageUrl(request.getSeatMapImageUrl())
                .enableFaceRecognition(request.getEnableFaceRecognition())
                .faceRecognitionThreshold(request.getFaceRecognitionThreshold())
                .requireFaceUpload(request.getRequireFaceUpload())
                .build();

        event = eventRepository.save(event);
        log.info("Event created successfully with id: {}", event.getId());

        return mapToDetailDTO(event);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId"),
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, allEntries = true)
    })
    public EventDetailDTO updateEvent(Long eventId, EventRequest request) {
        log.info("Updating event id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = getCurrentUser();

        if (!event.getOrganizer().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa sự kiện này");
        }

        if (event.getStatus() != EventStatus.DRAFT && !currentUser.isAdmin()) {
            throw new BadRequestException("Chỉ có thể chỉnh sửa sự kiện ở trạng thái DRAFT");
        }

        if (!event.getSlug().equals(request.getSlug()) && eventRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Slug sự kiện đã tồn tại: " + request.getSlug());
        }

        if (!event.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            event.setCategory(category);
        }

        event.setTitle(request.getTitle());
        event.setSlug(request.getSlug());
        event.setDescription(request.getDescription());
        event.setPosterUrl(request.getPosterUrl());
        event.setBannerUrl(request.getBannerUrl());
        event.setLocation(request.getLocation());
        event.setVenue(request.getVenue());
        event.setAddress(request.getAddress());
        event.setCity(request.getCity());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setEventType(request.getEventType());
        event.setMaxTicketsPerOrder(request.getMaxTicketsPerOrder());
        event.setMaxTicketsPerUser(request.getMaxTicketsPerUser());
        event.setEnableSeatSelection(request.getEnableSeatSelection());
        event.setSeatMapImageUrl(request.getSeatMapImageUrl());
        event.setEnableFaceRecognition(request.getEnableFaceRecognition());
        event.setFaceRecognitionThreshold(request.getFaceRecognitionThreshold());
        event.setRequireFaceUpload(request.getRequireFaceUpload());

        event = eventRepository.save(event);
        log.info("Event updated successfully with id: {}", event.getId());

        return mapToDetailDTO(event);
    }

    @Transactional
    public EventDetailDTO submitForApproval(Long eventId) {
        log.info("Submitting event for approval: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = getCurrentUser();

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không có quyền gửi duyệt sự kiện này");
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể gửi duyệt sự kiện ở trạng thái DRAFT");
        }

        // Validate event has at least one ticket zone
        if (event.getTicketZones() == null || event.getTicketZones().isEmpty()) {
            throw new BadRequestException("Sự kiện phải có ít nhất một khu vực vé trước khi gửi duyệt");
        }

        event.setStatus(EventStatus.PENDING);
        event = eventRepository.save(event);

        log.info("Event submitted for approval successfully with id: {}", event.getId());
        return mapToDetailDTO(event);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId"),
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, allEntries = true)
    })
    public EventDetailDTO approveOrRejectEvent(Long eventId, EventApprovalRequest request) {
        log.info("Processing approval for event id: {} with action: {}", eventId, request.getAction());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() != EventStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể duyệt/từ chối sự kiện ở trạng thái PENDING");
        }

        String action = request.getAction().toUpperCase();

        if ("APPROVE".equals(action)) {
            event.setStatus(EventStatus.APPROVED);
            log.info("Event approved with id: {}", event.getId());

            emailService.sendEventApprovedEmail(event);

        } else if ("REJECT".equals(action)) {
            event.setStatus(EventStatus.DRAFT);
            log.info("Event rejected with id: {}. Reason: {}", event.getId(), request.getReason());

            emailService.sendEventRejectedEmail(event, request.getReason());

        } else {
            throw new BadRequestException("Hành động không hợp lệ. Phải là APPROVE hoặc REJECT");
        }

        event = eventRepository.save(event);
        return mapToDetailDTO(event);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId"),
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, allEntries = true)
    })
    public EventDetailDTO publishEvent(Long eventId) {
        log.info("Publishing event id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = getCurrentUser();

        if (!event.getOrganizer().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xuất bản sự kiện này");
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể xuất bản sự kiện đã được duyệt");
        }

        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);

        log.info("Event published successfully with id: {}", event.getId());
        return mapToDetailDTO(event);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId"),
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, allEntries = true)
    })
    public EventDetailDTO toggleFeatured(Long eventId) {
        log.info("Toggling featured status for event id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        event.setIsFeatured(!event.getIsFeatured());
        event = eventRepository.save(event);

        log.info("Event featured status toggled to: {} for id: {}", event.getIsFeatured(), event.getId());
        return mapToDetailDTO(event);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId"),
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, allEntries = true)
    })
    public EventDetailDTO cancelEvent(Long eventId, String reason) {
        log.info("Cancelling event id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User currentUser = getCurrentUser();

        if (!event.getOrganizer().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền hủy sự kiện này");
        }

        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Không thể hủy sự kiện đã bắt đầu");
        }

        event.setStatus(EventStatus.CANCELLED);
        event = eventRepository.save(event);

        // Gửi email thông báo hủy sự kiện cho tất cả người có vé (async)
        List<Ticket> activeTickets = ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.ACTIVE);
        for (Ticket ticket : activeTickets) {
            emailService.sendEventCancelledEmail(ticket, reason);
        }

        // Auto-refund các order COMPLETED. Mỗi refund chạy trong transaction riêng
        // (RefundService.refundOrder dùng REQUIRES_NEW) để 1 order fail không rollback
        // toàn bộ thao tác hủy event.
        List<Order> paidOrders = orderRepository.findByEventIdAndPaymentStatus(eventId, PaymentStatus.COMPLETED);
        String refundReason = "Sự kiện bị hủy"
                + (reason != null && !reason.isBlank() ? ": " + reason : "");
        int refundSuccess = 0;
        int refundFail = 0;
        for (Order paid : paidOrders) {
            try {
                refundService.refundOrder(paid.getId(), refundReason);
                refundSuccess++;
            } catch (Exception ex) {
                refundFail++;
                log.error("Auto-refund failed for order {} (event {}): {}",
                        paid.getId(), eventId, ex.getMessage());
            }
        }
        log.info("Event {} cancelled, auto-refund: success={}, fail={}", eventId, refundSuccess, refundFail);

        return mapToDetailDTO(event);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId"),
            @CacheEvict(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, allEntries = true)
    })
    public void deleteEvent(Long eventId) {
        log.info("Deleting event id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getTotalTicketsSold() > 0) {
            throw new BadRequestException("Không thể xóa sự kiện đã bán vé. Hãy hủy sự kiện thay vì xóa.");
        }

        eventRepository.delete(event);
        log.info("Event deleted successfully with id: {}", eventId);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private EventListDTO mapToListDTO(Event event) {
        return EventListDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .slug(event.getSlug())
                .posterUrl(event.getPosterUrl())
                .location(event.getLocation())
                .venue(event.getVenue())
                .city(event.getCity())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .status(event.getStatus())
                .eventType(event.getEventType())
                .isFeatured(event.getIsFeatured())
                .viewCount(event.getViewCount())
                .totalTicketsSold(event.getTotalTicketsSold())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFullName())
                .organizerEmail(event.getOrganizer().getEmail())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private EventDetailDTO mapToDetailDTO(Event event) {
        // Calculate average rating from reviews - mặc định 0 nếu chưa có review
        Double averageRating = 0.0;
        if (event.getReviews() != null && !event.getReviews().isEmpty()) {
            averageRating = event.getReviews().stream()
                    .mapToInt(r -> r.getRating())
                    .average()
                    .orElse(0.0);
        }

        return EventDetailDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .slug(event.getSlug())
                .description(event.getDescription())
                .posterUrl(event.getPosterUrl())
                .bannerUrl(event.getBannerUrl())
                .location(event.getLocation())
                .venue(event.getVenue())
                .address(event.getAddress())
                .city(event.getCity())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .status(event.getStatus())
                .eventType(event.getEventType())
                .isFeatured(event.getIsFeatured())
                .maxTicketsPerOrder(event.getMaxTicketsPerOrder())
                .maxTicketsPerUser(event.getMaxTicketsPerUser())
                .enableSeatSelection(event.getEnableSeatSelection())
                .seatMapImageUrl(event.getSeatMapImageUrl())
                .enableFaceRecognition(event.getEnableFaceRecognition())
                .faceRecognitionThreshold(event.getFaceRecognitionThreshold())
                .requireFaceUpload(event.getRequireFaceUpload())
                .viewCount(event.getViewCount())
                .totalTicketsSold(event.getTotalTicketsSold())
                .totalRevenue(event.getTotalRevenue())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFullName())
                .organizerEmail(event.getOrganizer().getEmail())
                .organizerPhone(event.getOrganizer().getPhone())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .categorySlug(event.getCategory().getSlug())
                .totalTicketZones(event.getTicketZones().size())
                .totalOrders(event.getOrders().size())
                .totalReviews(event.getReviews().size())
                .averageRating(averageRating)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}