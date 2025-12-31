package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.event.EventApprovalRequest;
import com.gnxrt.ticketgoapi.dto.response.event.EventDetailDTO;
import com.gnxrt.ticketgoapi.dto.response.event.EventListDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import com.gnxrt.ticketgoapi.service.EventManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventManagementService eventManagementService;

    /**
     * GET /api/admin/events?page=0&size=10&status=PENDING&categoryId=1
     */
    @GetMapping
    public ResponseEntity<Page<EventListDTO>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) String keyword
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EventListDTO> events = eventManagementService.getAllEvents(
                status, categoryId, organizerId, eventType, city, isFeatured, keyword, pageable
        );

        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/admin/events/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<EventListDTO>> getPendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<EventListDTO> events = eventManagementService.getPendingEvents(pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/admin/events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDTO> getEventDetail(@PathVariable Long id) {
        EventDetailDTO event = eventManagementService.getEventDetail(id);
        return ResponseEntity.ok(event);
    }

    /**
     * POST /api/admin/events/{id}/approval
     */
    @PostMapping("/{id}/approval")
    public ResponseEntity<EventDetailDTO> approveOrRejectEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventApprovalRequest request
    ) {
        EventDetailDTO event = eventManagementService.approveOrRejectEvent(id, request);
        return ResponseEntity.ok(event);
    }

    /**
     * PATCH /api/admin/events/{id}/toggle-featured
     */
    @PatchMapping("/{id}/toggle-featured")
    public ResponseEntity<EventDetailDTO> toggleFeatured(@PathVariable Long id) {
        EventDetailDTO event = eventManagementService.toggleFeatured(id);
        return ResponseEntity.ok(event);
    }

    /**
     * POST /api/admin/events/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventDetailDTO> cancelEvent(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        EventDetailDTO event = eventManagementService.cancelEvent(id, reason);
        return ResponseEntity.ok(event);
    }

    /**
     * DELETE /api/admin/events/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventManagementService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/admin/events/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getEventStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalPending", eventManagementService.countPendingEvents());
        // TODO: Add more statistics

        return ResponseEntity.ok(stats);
    }
}