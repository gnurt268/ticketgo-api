package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.CacheConfig;
import com.gnxrt.ticketgoapi.dto.response.event.EventSummaryDTO;
import com.gnxrt.ticketgoapi.dto.response.event.PublicEventDetailDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Review;
import com.gnxrt.ticketgoapi.model.TicketZone;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.ReviewRepository;
import com.gnxrt.ticketgoapi.repository.TicketZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketZoneRepository ticketZoneRepository;
    private final ReviewRepository reviewRepository;

    public Page<EventSummaryDTO> getPublishedEvents(Pageable pageable) {
        log.info("Getting published events");
        return mapPageBatch(eventRepository.findUpcomingPublishedEvents(LocalDateTime.now(), pageable));
    }

    public Page<EventSummaryDTO> getEventsByCategory(Long categoryId, Pageable pageable) {
        log.info("Getting events by category: {}", categoryId);
        return mapPageBatch(eventRepository.findUpcomingPublishedEventsByCategory(categoryId, LocalDateTime.now(), pageable));
    }

    public Page<EventSummaryDTO> getEventsByCity(String city, Pageable pageable) {
        log.info("Getting events by city: {}", city);
        return mapPageBatch(eventRepository.findEventsByCity(city, LocalDateTime.now(), pageable));
    }

    public Page<EventSummaryDTO> searchEvents(String keyword, Pageable pageable) {
        log.info("Searching events with keyword: {}", keyword);
        return mapPageBatch(eventRepository.searchPublishedEvents(keyword, LocalDateTime.now(), pageable));
    }

    public Page<EventSummaryDTO> getFeaturedEvents(Pageable pageable) {
        log.info("Getting featured events");
        return mapPageBatch(eventRepository.findFeaturedEvents(LocalDateTime.now(), pageable));
    }

    public Page<EventSummaryDTO> getTopSellingEvents(Pageable pageable) {
        log.info("Getting top selling events");
        return mapPageBatch(eventRepository.findTopSellingEvents(pageable));
    }

    public Page<EventSummaryDTO> getMostViewedEvents(Pageable pageable) {
        log.info("Getting most viewed events");
        return mapPageBatch(eventRepository.findMostViewedEvents(pageable));
    }

    public Page<EventSummaryDTO> searchAndFilterEvents(
            String keyword,
            Long categoryId,
            String city,
            EventType eventType,
            Boolean isFeatured,
            String sortBy,
            String sortDirection,
            Pageable pageable
    ) {
        log.info("Searching and filtering events - keyword: {}, categoryId: {}, city: {}", keyword, categoryId, city);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return mapPageBatch(eventRepository.searchPublishedEvents(keyword, LocalDateTime.now(), pageable));
        }

        Page<Event> events = eventRepository.findByFilters(
                EventStatus.PUBLISHED,
                categoryId,
                null,
                eventType,
                city,
                isFeatured,
                LocalDateTime.now(),
                pageable
        );

        return mapPageBatch(events);
    }

    @Transactional
    @Cacheable(value = CacheConfig.CACHE_EVENT_DETAIL, key = "#eventId")
    public PublicEventDetailDTO getEventDetail(Long eventId) {
        log.info("Getting event detail for id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Sự kiện không khả dụng");
        }

        event.incrementViewCount();
        eventRepository.save(event);

        return mapToDetailDTO(event);
    }

    @Transactional
    @Cacheable(value = CacheConfig.CACHE_EVENT_DETAIL_SLUG, key = "#slug")
    public PublicEventDetailDTO getEventDetailBySlug(String slug) {
        log.info("Getting event detail for slug: {}", slug);

        Event event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", slug));

        EventStatus status = event.getStatus();
        if (status != EventStatus.PUBLISHED
                && status != EventStatus.COMPLETED
                && status != EventStatus.CANCELLED) {
            throw new BadRequestException("Sự kiện không khả dụng");
        }

        event.incrementViewCount();
        eventRepository.save(event);

        return mapToDetailDTO(event);
    }

    public List<EventSummaryDTO> getRelatedEvents(Long eventId, int limit) {
        log.info("Getting related events for event id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        Pageable pageable = PageRequest.of(0, limit + 1);
        Page<Event> relatedEvents = eventRepository.findUpcomingPublishedEventsByCategory(
                event.getCategory().getId(),
                LocalDateTime.now(),
                pageable
        );

        List<Event> filtered = relatedEvents.getContent().stream()
                .filter(e -> !e.getId().equals(eventId))
                .limit(limit)
                .collect(Collectors.toList());
        return mapEventsBatch(filtered);
    }

    public List<String> getAvailableCities() {
        log.info("Getting available cities");
        return eventRepository.findDistinctCitiesOfPublishedEvents(LocalDateTime.now());
    }

    private Page<EventSummaryDTO> mapPageBatch(Page<Event> page) {
        List<EventSummaryDTO> dtos = mapEventsBatch(page.getContent());
        return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
    }

    private List<EventSummaryDTO> mapEventsBatch(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, ZoneStats> zoneStats = new HashMap<>();
        for (Object[] row : ticketZoneRepository.findStatsByEventIds(ids)) {
            Long eid = (Long) row[0];
            zoneStats.put(eid, new ZoneStats(
                    (BigDecimal) row[1],
                    (BigDecimal) row[2],
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).intValue()
            ));
        }

        Map<Long, ReviewStats> reviewStats = new HashMap<>();
        for (Object[] row : reviewRepository.findStatsByEventIds(ids)) {
            Long eid = (Long) row[0];
            reviewStats.put(eid, new ReviewStats(
                    row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                    ((Number) row[2]).longValue()
            ));
        }

        ZoneStats emptyZone = new ZoneStats(null, null, 0, 0);
        ReviewStats emptyReview = new ReviewStats(0.0, 0L);

        return events.stream()
                .map(event -> {
                    ZoneStats zs = zoneStats.getOrDefault(event.getId(), emptyZone);
                    ReviewStats rs = reviewStats.getOrDefault(event.getId(), emptyReview);
                    return EventSummaryDTO.builder()
                            .id(event.getId())
                            .title(event.getTitle())
                            .slug(event.getSlug())
                            .posterUrl(event.getPosterUrl())
                            .location(event.getLocation())
                            .venue(event.getVenue())
                            .city(event.getCity())
                            .startDate(event.getStartDate())
                            .endDate(event.getEndDate())
                            .eventType(event.getEventType())
                            .isFeatured(event.getIsFeatured())
                            .viewCount(event.getViewCount())
                            .categoryId(event.getCategory().getId())
                            .categoryName(event.getCategory().getName())
                            .categorySlug(event.getCategory().getSlug())
                            .organizerId(event.getOrganizer().getId())
                            .organizerName(event.getOrganizer().getFullName())
                            .minPrice(zs.minPrice())
                            .maxPrice(zs.maxPrice())
                            .currency("VND")
                            .totalCapacity(zs.totalCapacity())
                            .availableCapacity(zs.availableCapacity())
                            .isSoldOut(zs.availableCapacity() == 0)
                            .averageRating(rs.average())
                            .totalReviews((int) rs.count())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private record ZoneStats(BigDecimal minPrice, BigDecimal maxPrice,
                             int totalCapacity, int availableCapacity) {}

    private record ReviewStats(double average, long count) {}

    private PublicEventDetailDTO mapToDetailDTO(Event event) {
        List<TicketZone> ticketZones = ticketZoneRepository.findByEventIdAndIsActiveTrueOrderByDisplayOrderAsc(event.getId());
        List<PublicEventDetailDTO.TicketZoneDTO> ticketZoneDTOs = ticketZones.stream()
                .map(this::mapTicketZoneToDTO)
                .collect(Collectors.toList());

        List<Review> recentReviews = reviewRepository.findRecentApprovedReviews(
                event.getId(),
                PageRequest.of(0, 5)
        );
        List<PublicEventDetailDTO.ReviewSummaryDTO> reviewDTOs = recentReviews.stream()
                .map(this::mapReviewToDTO)
                .collect(Collectors.toList());

        Integer totalCapacity = ticketZoneRepository.getTotalCapacityByEventId(event.getId());
        Integer availableCapacity = ticketZoneRepository.getAvailableCapacityByEventId(event.getId());

        Double averageRating = reviewRepository.getAverageRatingByEventId(event.getId());
        Long totalReviews = reviewRepository.countByEventIdAndIsApprovedTrue(event.getId());

        return PublicEventDetailDTO.builder()
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
                .eventType(event.getEventType())
                .isFeatured(event.getIsFeatured())
                .maxTicketsPerOrder(event.getMaxTicketsPerOrder())
                .enableSeatSelection(event.getEnableSeatSelection())
                .seatMapImageUrl(event.getSeatMapImageUrl())
                .enableFaceRecognition(event.getEnableFaceRecognition())
                .requireFaceUpload(event.getRequireFaceUpload())
                .viewCount(event.getViewCount())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .categorySlug(event.getCategory().getSlug())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFullName())
                .organizerAvatarUrl(event.getOrganizer().getAvatarUrl())
                .ticketZones(ticketZoneDTOs)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .totalReviews(totalReviews != null ? totalReviews.intValue() : 0)
                .recentReviews(reviewDTOs)
                .totalCapacity(totalCapacity != null ? totalCapacity : 0)
                .availableCapacity(availableCapacity != null ? availableCapacity : 0)
                .isSoldOut(availableCapacity != null && availableCapacity == 0)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private PublicEventDetailDTO.TicketZoneDTO mapTicketZoneToDTO(TicketZone zone) {
        return PublicEventDetailDTO.TicketZoneDTO.builder()
                .id(zone.getId())
                .zoneName(zone.getZoneName())
                .zoneCode(zone.getZoneCode())
                .description(zone.getDescription())
                .colorCode(zone.getColorCode())
                .price(zone.getPrice())
                .currency(zone.getCurrency())
                .totalCapacity(zone.getTotalCapacity())
                .availableCapacity(zone.getAvailableCapacity())
                .zoneType(zone.getZoneType().name())
                .isActive(zone.getIsActive())
                .displayOrder(zone.getDisplayOrder())
                .saleStartDate(zone.getSaleStartDate())
                .saleEndDate(zone.getSaleEndDate())
                .isSaleActive(zone.isSaleActive())
                .isSoldOut(zone.getAvailableCapacity() == 0)
                .build();
    }

    private PublicEventDetailDTO.ReviewSummaryDTO mapReviewToDTO(Review review) {
        return PublicEventDetailDTO.ReviewSummaryDTO.builder()
                .id(review.getId())
                .userName(review.getUser().getFullName())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .sentimentLabel(review.getSentimentLabel() != null ? review.getSentimentLabel().name() : null)
                .createdAt(review.getCreatedAt())
                .build();
    }
}