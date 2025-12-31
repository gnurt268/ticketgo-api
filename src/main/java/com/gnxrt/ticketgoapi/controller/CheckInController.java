package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.checkin.CheckInRequest;
import com.gnxrt.ticketgoapi.dto.response.checkin.CheckInResponse;
import com.gnxrt.ticketgoapi.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * POST /api/checkin/validate
     * Validate QR code without check-in (preview ticket info)
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('STAFF', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CheckInResponse> validateQR(@Valid @RequestBody CheckInRequest request) {
        log.info("Validating QR code");
        CheckInResponse response = checkInService.validateQR(request.getQrContent());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/checkin/scan
     * Check-in ticket by QR code
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('STAFF', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CheckInResponse> checkIn(
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication
    ) {
        String staffEmail = authentication.getName();
        log.info("Check-in request from staff: {}", staffEmail);

        CheckInResponse response = checkInService.checkIn(request.getQrContent(), staffEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/checkin/stats/{eventId}
     * Get check-in statistics for an event
     */
    @GetMapping("/stats/{eventId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<CheckInResponse> getEventStats(@PathVariable Long eventId) {
        log.info("Getting check-in stats for event: {}", eventId);
        CheckInResponse response = checkInService.getEventStats(eventId);
        return ResponseEntity.ok(response);
    }
}