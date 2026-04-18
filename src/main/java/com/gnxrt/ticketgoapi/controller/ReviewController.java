package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.review.CreateReviewRequest;
import com.gnxrt.ticketgoapi.dto.response.review.EventReviewSummaryDTO;
import com.gnxrt.ticketgoapi.dto.response.review.MyReviewDTO;
import com.gnxrt.ticketgoapi.dto.response.review.ReviewDTO;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import com.gnxrt.ticketgoapi.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/event/{eventId}/summary")
    public ResponseEntity<EventReviewSummaryDTO> getEventReviewSummary(@PathVariable Long eventId) {
        Long userId = currentUserIdOrNull();
        EventReviewSummaryDTO summary = reviewService.getEventReviewSummary(eventId, userId);
        return ResponseEntity.ok(summary);
    }

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

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MyReviewDTO>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MyReviewDTO> reviews = reviewService.getMyReviews(currentUser().getId(), pageable);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody CreateReviewRequest request) {
        ReviewDTO review = reviewService.createReview(currentUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewDTO review = reviewService.updateReview(currentUser().getId(), id, request);
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(currentUser().getId(), id);
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
