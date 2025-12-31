package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.organizer.OrganizerRequestReviewRequest;
import com.gnxrt.ticketgoapi.dto.response.organizer.OrganizerRequestDTO;
import com.gnxrt.ticketgoapi.enums.OrganizerRequestStatus;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.service.OrganizerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 *
 */
@RestController
@RequestMapping("/api/admin/organizer-requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrganizerController {

    private final OrganizerRequestService organizerRequestService;

    /**
     * GET /api/admin/organizer-requests
     */
    @GetMapping
    public ResponseEntity<Page<OrganizerRequestDTO>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) OrganizerRequestStatus status,
            @RequestParam(required = false) String keyword
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrganizerRequestDTO> requests = organizerRequestService.getAllRequests(status, keyword, pageable);
        return ResponseEntity.ok(requests);
    }

    /**
     * GET /api/admin/organizer-requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<OrganizerRequestDTO>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<OrganizerRequestDTO> requests = organizerRequestService.getPendingRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    /**
     * GET /api/admin/organizer-requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizerRequestDTO> getRequestDetail(@PathVariable Long id) {
        OrganizerRequestDTO request = organizerRequestService.getRequestById(id);
        return ResponseEntity.ok(request);
    }

    /**
     * POST /api/admin/organizer-requests/{id}/review
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<OrganizerRequestDTO> reviewRequest(
            @PathVariable Long id,
            @Valid @RequestBody OrganizerRequestReviewRequest request,
            @AuthenticationPrincipal User admin
    ) {
        OrganizerRequestDTO result = organizerRequestService.reviewRequest(id, admin.getId(), request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/organizer-requests/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(organizerRequestService.getStatistics());
    }
}