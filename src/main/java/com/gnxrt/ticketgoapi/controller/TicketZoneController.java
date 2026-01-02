package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.ticketzone.TicketZoneRequest;
import com.gnxrt.ticketgoapi.dto.response.ticketzone.TicketZoneDTO;
import com.gnxrt.ticketgoapi.service.TicketZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketZoneController {

    private final TicketZoneService ticketZoneService;

    /**
     * GET /api/events/{eventId}/zones
     */
    @GetMapping("/events/{eventId}/zones")
    public ResponseEntity<List<TicketZoneDTO>> getEventZones(@PathVariable Long eventId) {
        List<TicketZoneDTO> zones = ticketZoneService.getActiveZonesByEventId(eventId);
        return ResponseEntity.ok(zones);
    }

    /**
     * GET /api/zones/{id}
     */
    @GetMapping("/zones/{id}")
    public ResponseEntity<TicketZoneDTO> getZoneById(@PathVariable Long id) {
        TicketZoneDTO zone = ticketZoneService.getZoneById(id);
        return ResponseEntity.ok(zone);
    }

    /**
     * GET /api/organizer/events/{eventId}/zones
     */
    @GetMapping("/organizer/events/{eventId}/zones")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<TicketZoneDTO>> getAllEventZones(@PathVariable Long eventId) {
        List<TicketZoneDTO> zones = ticketZoneService.getZonesByEventId(eventId);
        return ResponseEntity.ok(zones);
    }

    /**
     * POST /api/organizer/events/{eventId}/zones
     */
    @PostMapping("/organizer/events/{eventId}/zones")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<TicketZoneDTO> createZone(
            @PathVariable Long eventId,
            @Valid @RequestBody TicketZoneRequest request
    ) {
        TicketZoneDTO zone = ticketZoneService.createZone(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(zone);
    }

    /**
     * PUT /api/organizer/zones/{id}
     */
    @PutMapping("/organizer/zones/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<TicketZoneDTO> updateZone(
            @PathVariable Long id,
            @Valid @RequestBody TicketZoneRequest request
    ) {
        TicketZoneDTO zone = ticketZoneService.updateZone(id, request);
        return ResponseEntity.ok(zone);
    }

    /**
     * DELETE /api/organizer/zones/{id}
     */
    @DeleteMapping("/organizer/zones/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        ticketZoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/organizer/zones/{id}/toggle-active
     */
    @PatchMapping("/organizer/zones/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<TicketZoneDTO> toggleActive(@PathVariable Long id) {
        TicketZoneDTO zone = ticketZoneService.toggleActive(id);
        return ResponseEntity.ok(zone);
    }

    /**
     * PUT /api/organizer/events/{eventId}/zones/reorder
     */
    @PutMapping("/organizer/events/{eventId}/zones/reorder")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> updateDisplayOrders(
            @PathVariable Long eventId,
            @RequestBody List<Long> zoneIds
    ) {
        ticketZoneService.updateDisplayOrders(eventId, zoneIds);
        return ResponseEntity.ok().build();
    }
}