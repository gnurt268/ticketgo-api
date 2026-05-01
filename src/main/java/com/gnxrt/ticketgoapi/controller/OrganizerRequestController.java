package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.organizer.OrganizerRegistrationRequest;
import com.gnxrt.ticketgoapi.dto.response.organizer.OrganizerRequestDTO;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import com.gnxrt.ticketgoapi.service.OrganizerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer-requests")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OrganizerRequestController {

    private final OrganizerRequestService organizerRequestService;
    private final UserRepository userRepository;

    /**
     * POST /api/organizer-requests
     */
    @PostMapping
    public ResponseEntity<OrganizerRequestDTO> submitRequest(
            @Valid @RequestBody OrganizerRegistrationRequest request
    ) {
        User currentUser = getCurrentUser();
        OrganizerRequestDTO result = organizerRequestService.submitRequest(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /api/organizer-requests/my
     */
    @GetMapping("/my")
    public ResponseEntity<OrganizerRequestDTO> getMyRequest() {
        User currentUser = getCurrentUser();
        OrganizerRequestDTO result = organizerRequestService.getMyRequest(currentUser.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/organizer-requests/my
     */
    @DeleteMapping("/my")
    public ResponseEntity<Void> cancelMyRequest() {
        User currentUser = getCurrentUser();
        organizerRequestService.cancelMyRequest(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
