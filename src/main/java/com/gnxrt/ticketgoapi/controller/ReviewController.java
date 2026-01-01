package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.review.CreateReviewRequest;
import com.gnxrt.ticketgoapi.dto.response.review.EventReviewSummaryDTO;
import com.gnxrt.ticketgoapi.dto.response.review.ReviewDTO;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * GET /api/reviews/event/{eventId}/summary
     */
    @GetMapping("/event/{eventId}/summary")
    public ResponseEntity<EventReviewSummaryDTO> getEventReviewSummary(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User user
    ) {
        Long userId = user != null ? user.getId() : null;
        EventReviewSummaryDTO summary = reviewService.getEventReviewSummary(eventId, userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/reviews/event/{eventId}
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Page<ReviewDTO>> getEventReviews(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewDTO> reviews = reviewService.getEventReviews(eventId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * POST /api/reviews
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        ReviewDTO review = reviewService.createReview(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * PUT /api/reviews/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        ReviewDTO review = reviewService.updateReview(user.getId(), id, request);
        return ResponseEntity.ok(review);
    }

    /**
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        reviewService.deleteReview(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}