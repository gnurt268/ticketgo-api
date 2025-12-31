package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.response.event.EventSummaryDTO;
import com.gnxrt.ticketgoapi.dto.response.event.PublicEventDetailDTO;
import com.gnxrt.ticketgoapi.enums.EventType;
import com.gnxrt.ticketgoapi.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * GET /api/events
     */
    @GetMapping
    public ResponseEntity<Page<EventSummaryDTO>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) Boolean isFeatured
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EventSummaryDTO> events = eventService.searchAndFilterEvents(
                keyword, categoryId, city, eventType, isFeatured,
                sortBy, sortDirection, pageable
        );

        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/search?q=keyword
     */
    @GetMapping("/search")
    public ResponseEntity<Page<EventSummaryDTO>> searchEvents(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        Page<EventSummaryDTO> events = eventService.searchEvents(keyword, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<Page<EventSummaryDTO>> getFeaturedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        Page<EventSummaryDTO> events = eventService.getFeaturedEvents(pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/top-selling
     */
    @GetMapping("/top-selling")
    public ResponseEntity<Page<EventSummaryDTO>> getTopSellingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventSummaryDTO> events = eventService.getTopSellingEvents(pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/most-viewed
     */
    @GetMapping("/most-viewed")
    public ResponseEntity<Page<EventSummaryDTO>> getMostViewedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventSummaryDTO> events = eventService.getMostViewedEvents(pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<EventSummaryDTO>> getEventsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        Page<EventSummaryDTO> events = eventService.getEventsByCategory(categoryId, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/city/{city}
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<Page<EventSummaryDTO>> getEventsByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        Page<EventSummaryDTO> events = eventService.getEventsByCity(city, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/events/cities
     */
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAvailableCities() {
        List<String> cities = eventService.getAvailableCities();
        return ResponseEntity.ok(cities);
    }

    /**
     * GET /api/events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicEventDetailDTO> getEventDetail(@PathVariable Long id) {
        PublicEventDetailDTO event = eventService.getEventDetail(id);
        return ResponseEntity.ok(event);
    }

    /**
     * GET /api/events/slug/{slug}
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<PublicEventDetailDTO> getEventDetailBySlug(@PathVariable String slug) {
        PublicEventDetailDTO event = eventService.getEventDetailBySlug(slug);
        return ResponseEntity.ok(event);
    }

    /**
     * GET /api/events/{id}/related
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<EventSummaryDTO>> getRelatedEvents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit
    ) {
        List<EventSummaryDTO> events = eventService.getRelatedEvents(id, limit);
        return ResponseEntity.ok(events);
    }
}