package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.event.EventRequest;
import com.gnxrt.ticketgoapi.dto.response.event.EventDetailDTO;
import com.gnxrt.ticketgoapi.service.EventManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer/events")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
@RequiredArgsConstructor
public class OrganizerEventController {

    private final EventManagementService eventManagementService;

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
}