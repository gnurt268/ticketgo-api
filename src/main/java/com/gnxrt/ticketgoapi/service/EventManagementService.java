package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.event.EventApprovalRequest;
import com.gnxrt.ticketgoapi.dto.request.event.EventRequest;
import com.gnxrt.ticketgoapi.dto.response.event.EventDetailDTO;
import com.gnxrt.ticketgoapi.dto.response.event.EventListDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ForbiddenException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Category;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.CategoryRepository;
import com.gnxrt.ticketgoapi.repository.EventRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class EventManagementService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

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
                    status, categoryId, organizerId, eventType, city, isFeatured, pageable
            );
        }

        return events.map(this::mapToListDTO);
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

        // TODO: Validate event has at least one ticket zone
        // if (event.getTicketZones().isEmpty()) {
        //     throw new RuntimeException("Event must have at least one ticket zone");
        // }

        event.setStatus(EventStatus.PENDING);
        event = eventRepository.save(event);

        log.info("Event submitted for approval successfully with id: {}", event.getId());
        return mapToDetailDTO(event);
    }

    @Transactional
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
        } else if ("REJECT".equals(action)) {
            event.setStatus(EventStatus.DRAFT);
            log.info("Event rejected with id: {}. Reason: {}", event.getId(), request.getReason());
            // TODO: Send email notification to organizer with rejection reason
        } else {
            throw new BadRequestException("Hành động không hợp lệ. Phải là APPROVE hoặc REJECT");
        }

        event = eventRepository.save(event);
        return mapToDetailDTO(event);
    }

    @Transactional
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

        // TODO: Send email notifications to ticket holders
        // TODO: Process refunds

        log.info("Event cancelled successfully with id: {}", event.getId());
        return mapToDetailDTO(event);
    }

    @Transactional
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
        // TODO: Calculate average rating from reviews
        Double averageRating = 0.0;

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