package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.checkin.CheckInRequest;
import com.gnxrt.ticketgoapi.dto.response.checkin.CheckInResponse;
import com.gnxrt.ticketgoapi.dto.response.checkin.CheckinEventDTO;
import com.gnxrt.ticketgoapi.service.CheckInService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ORGANIZER', 'ADMIN')")
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * GET /api/checkin/events — danh sách sự kiện current-user được phép check-in.
     */
    @GetMapping("/events")
    public ResponseEntity<List<CheckinEventDTO>> getMyEvents(Authentication authentication) {
        return ResponseEntity.ok(checkInService.listCheckinableEvents(authentication.getName()));
    }

    /**
     * GET /api/checkin/events/{eventId}/stats
     */
    @GetMapping("/events/{eventId}/stats")
    public ResponseEntity<CheckInResponse> getEventStats(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(checkInService.getEventStats(eventId, authentication.getName()));
    }

    /**
     * POST /api/checkin/events/{eventId}/validate — preview vé (không ghi nhận).
     */
    @PostMapping("/events/{eventId}/validate")
    public ResponseEntity<CheckInResponse> validateQR(
            @PathVariable Long eventId,
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                checkInService.validateQR(eventId, request.getQrContent(), authentication.getName()));
    }

    /**
     * POST /api/checkin/events/{eventId}/scan — check-in vé.
     */
    @PostMapping("/events/{eventId}/scan")
    public ResponseEntity<CheckInResponse> checkIn(
            @PathVariable Long eventId,
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ip = extractClientIp(httpRequest);
        CheckInResponse response = checkInService.checkIn(
                eventId, request.getQrContent(), authentication.getName(), deviceInfo, ip);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
