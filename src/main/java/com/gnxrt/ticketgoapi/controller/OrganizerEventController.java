package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.event.EventRequest;
import com.gnxrt.ticketgoapi.dto.response.event.EventDetailDTO;
import com.gnxrt.ticketgoapi.dto.response.event.EventListDTO;
import com.gnxrt.ticketgoapi.dto.response.organizer.OrganizerDashboardDTO;
import com.gnxrt.ticketgoapi.dto.response.organizer.RevenueStatisticsDTO;
import com.gnxrt.ticketgoapi.dto.request.eventstaff.AssignStaffRequest;
import com.gnxrt.ticketgoapi.dto.response.eventstaff.EventStaffDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.service.EventManagementService;
import com.gnxrt.ticketgoapi.service.EventStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizer/events")
@PreAuthorize("hasRole('ORGANIZER')")
@RequiredArgsConstructor
public class OrganizerEventController {

    private final EventManagementService eventManagementService;
    private final EventStaffService eventStaffService;

    /**
     * ORGANIZER
     * GET /api/organizer/events
     */
    @GetMapping
    public ResponseEntity<Page<EventListDTO>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String keyword
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EventListDTO> events = eventManagementService.getMyEvents(status, keyword, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * ORGANIZER
     * GET /api/organizer/events/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMyStatistics() {
        return ResponseEntity.ok(eventManagementService.getMyEventStatistics());
    }

    /**
     * ORGANIZER
     * GET /api/organizer/events/statistics/revenue?period=7d|30d|90d
     */
    @GetMapping("/statistics/revenue")
    public ResponseEntity<RevenueStatisticsDTO> getRevenueStatistics(
            @RequestParam(defaultValue = "7d") String period
    ) {
        return ResponseEntity.ok(eventManagementService.getRevenueStatistics(period));
    }

    /**
     * ORGANIZER
     * GET /api/organizer/events/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<OrganizerDashboardDTO> getDashboard() {
        return ResponseEntity.ok(eventManagementService.getOrganizerDashboard());
    }

    /**
     * ORGANIZER
     * POST /api/organizer/events
     */
    @PostMapping
    public ResponseEntity<EventDetailDTO> createEvent(@Valid @RequestBody EventRequest request) {
        EventDetailDTO event = eventManagementService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * ORGANIZER
     * PUT /api/organizer/events/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventDetailDTO> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request
    ) {
        EventDetailDTO event = eventManagementService.updateEvent(id, request);
        return ResponseEntity.ok(event);
    }

    /**
     * ORGANIZER
     * POST /api/organizer/events/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<EventDetailDTO> submitForApproval(@PathVariable Long id) {
        EventDetailDTO event = eventManagementService.submitForApproval(id);
        return ResponseEntity.ok(event);
    }

    /**
     * ORGANIZER
     * POST /api/organizer/events/{id}/publish
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<EventDetailDTO> publishEvent(@PathVariable Long id) {
        EventDetailDTO event = eventManagementService.publishEvent(id);
        return ResponseEntity.ok(event);
    }

    /**
     * ORGANIZER
     * POST /api/organizer/events/{id}/cancel
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
     * ORGANIZER
     * GET /api/organizer/events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDTO> getEventDetail(@PathVariable Long id) {
        EventDetailDTO event = eventManagementService.getEventDetail(id);
        return ResponseEntity.ok(event);
    }

    // ==================== Nhân viên check-in (EventStaff) ====================

    /**
     * GET /api/organizer/events/{eventId}/staff
     */
    @GetMapping("/{eventId}/staff")
    public ResponseEntity<List<EventStaffDTO>> getEventStaff(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventStaffService.getStaff(eventId));
    }

    /**
     * POST /api/organizer/events/{eventId}/staff
     */
    @PostMapping("/{eventId}/staff")
    public ResponseEntity<EventStaffDTO> assignStaff(
            @PathVariable Long eventId,
            @Valid @RequestBody AssignStaffRequest request
    ) {
        EventStaffDTO dto = eventStaffService.assignStaff(eventId, request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * DELETE /api/organizer/events/{eventId}/staff/{userId}
     */
    @DeleteMapping("/{eventId}/staff/{userId}")
    public ResponseEntity<Void> removeStaff(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        eventStaffService.removeStaff(eventId, userId);
        return ResponseEntity.noContent().build();
    }
}