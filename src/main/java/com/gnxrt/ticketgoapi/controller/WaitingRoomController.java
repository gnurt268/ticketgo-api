package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.waitingroom.CreateWaitingRoomRequest;
import com.gnxrt.ticketgoapi.dto.request.waitingroom.JoinQueueRequest;
import com.gnxrt.ticketgoapi.dto.response.waitingroom.JoinQueueResponse;
import com.gnxrt.ticketgoapi.dto.response.waitingroom.QueueStatusResponse;
import com.gnxrt.ticketgoapi.dto.response.waitingroom.WaitingRoomResponse;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import com.gnxrt.ticketgoapi.service.WaitingRoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/waiting-room")
@RequiredArgsConstructor
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;
    private final UserRepository userRepository;

    /**
     * POST /api/waiting-room
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<WaitingRoomResponse> createWaitingRoom(
            @Valid @RequestBody CreateWaitingRoomRequest request) {

        log.info("Creating waiting room for event {}", request.getEventId());
        WaitingRoomResponse response = waitingRoomService.createWaitingRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/waiting-room/event/{eventId}
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<WaitingRoomResponse> getWaitingRoomByEventId(
            @PathVariable Long eventId) {

        WaitingRoomResponse response = waitingRoomService.getWaitingRoomByEventId(eventId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/waiting-room/slug/{slug}
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<WaitingRoomResponse> getWaitingRoomBySlug(
            @PathVariable String slug) {

        WaitingRoomResponse response = waitingRoomService.getWaitingRoomBySlug(slug);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/waiting-room/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<WaitingRoomResponse> toggleWaitingRoom(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        log.info("Toggling waiting room {} to {}", id, enabled);
        WaitingRoomResponse response = waitingRoomService.toggleWaitingRoom(id, enabled);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/waiting-room/event/{eventId}/join
     */
    @PostMapping("/event/{eventId}/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JoinQueueResponse> joinQueue(
            @PathVariable Long eventId,
            @RequestBody(required = false) JoinQueueRequest joinRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        User user = getCurrentUser(userDetails);
        log.info("User {} joining queue for event {}, hasExistingToken: {}",
                user.getId(), eventId, joinRequest != null && joinRequest.getVisitorToken() != null);

        JoinQueueResponse response = waitingRoomService.joinQueue(eventId, user, request, joinRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/waiting-room/event/{eventId}/status
     */
    @GetMapping("/event/{eventId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getCurrentUser(userDetails);
        QueueStatusResponse response = waitingRoomService.getQueueStatus(eventId, user);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/waiting-room/event/{eventId}/enter
     */
    @PostMapping("/event/{eventId}/enter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QueueStatusResponse> enterProtectedZone(
            @PathVariable Long eventId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getCurrentUser(userDetails);
        String accessToken = body.get("accessToken");

        log.info("User {} entering protected zone for event {}", user.getId(), eventId);

        QueueStatusResponse response = waitingRoomService.enterProtectedZone(eventId, user, accessToken);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/waiting-room/event/{eventId}/leave
     */
    @PostMapping("/event/{eventId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> leaveQueue(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getCurrentUser(userDetails);
        log.info("User {} leaving queue for event {}", user.getId(), eventId);

        waitingRoomService.leaveQueue(eventId, user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bạn đã rời khỏi hàng chờ"
        ));
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}