package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.review.CreateReviewRequest;
import com.gnxrt.ticketgoapi.dto.response.review.EventReviewSummaryDTO;
import com.gnxrt.ticketgoapi.dto.response.review.MyReviewDTO;
import com.gnxrt.ticketgoapi.dto.response.review.ReviewDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ForbiddenException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Review;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.OrderRepository;
import com.gnxrt.ticketgoapi.repository.ReviewRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public EventReviewSummaryDTO getEventReviewSummary(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        Double avgRating = reviewRepository.getAverageRatingByEventId(eventId);
        Long totalReviews = reviewRepository.countByEventIdAndIsApprovedTrue(eventId);

        List<Object[]> ratingCounts = reviewRepository.countByRatingForEvent(eventId);
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        for (Object[] row : ratingCounts) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDistribution.put(rating, count);
        }

        List<Review> recentReviews = reviewRepository.findRecentApprovedReviews(eventId, PageRequest.of(0, 10));
        List<ReviewDTO> recentReviewDTOs = recentReviews.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        ReviewDTO userReview = null;
        boolean canReview = false;
        String cannotReviewReason = null;

        if (userId != null) {
            // Check if user already reviewed
            Review existingReview = reviewRepository.findByUserIdAndEventId(userId, eventId).orElse(null);
            if (existingReview != null) {
                userReview = mapToDTO(existingReview);
                cannotReviewReason = "Bạn đã đánh giá sự kiện này";
            } else {
                // Check if event has ended
                if (event.getEndDate().isAfter(LocalDateTime.now())) {
                    cannotReviewReason = "Sự kiện chưa kết thúc";
                } else {
                    // Check if user has ticket for this event
                    boolean hasTicket = orderRepository.existsByUserIdAndEventIdAndPaymentStatus(
                            userId, eventId, PaymentStatus.COMPLETED);
                    if (hasTicket) {
                        canReview = true;
                    } else {
                        cannotReviewReason = "Bạn cần có vé đã thanh toán để đánh giá";
                    }
                }
            }
        } else {
            cannotReviewReason = "Đăng nhập để đánh giá";
        }

        return EventReviewSummaryDTO.builder()
                .eventId(eventId)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalReviews(totalReviews)
                .ratingDistribution(ratingDistribution)
                .recentReviews(recentReviewDTOs)
                .userReview(userReview)
                .canReview(canReview)
                .cannotReviewReason(cannotReviewReason)
                .build();
    }

    public Page<ReviewDTO> getEventReviews(Long eventId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByEventIdAndIsApprovedTrueOrderByCreatedAtDesc(eventId, pageable);
        return reviews.map(this::mapToDTO);
    }

    public Page<MyReviewDTO> getMyReviews(Long userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return reviews.map(this::mapToMyReviewDTO);
    }

    private MyReviewDTO mapToMyReviewDTO(Review review) {
        Event event = review.getEvent();
        return MyReviewDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isApproved(review.getIsApproved())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventSlug(event.getSlug())
                .eventPosterUrl(event.getPosterUrl())
                .eventStartDate(event.getStartDate())
                .build();
    }

    @Transactional
    public ReviewDTO createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));

        if (event.getEndDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Sự kiện chưa kết thúc, không thể đánh giá");
        }

        boolean hasTicket = orderRepository.existsByUserIdAndEventIdAndPaymentStatus(
                userId, request.getEventId(), PaymentStatus.COMPLETED);
        if (!hasTicket) {
            throw new ForbiddenException("Bạn cần có vé đã thanh toán để đánh giá sự kiện này");
        }

        if (reviewRepository.existsByUserIdAndEventId(userId, request.getEventId())) {
            throw new ConflictException("Bạn đã đánh giá sự kiện này rồi");
        }

        Review review = Review.builder()
                .user(user)
                .event(event)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .isApproved(true) // Auto approve for now
                .isReported(false)
                .build();

        review = reviewRepository.save(review);

        log.info("User {} created review for event {}", userId, request.getEventId());
        return mapToDTO(review);
    }

    @Transactional
    public ReviewDTO updateReview(Long userId, Long reviewId, CreateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);

        return mapToDTO(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!review.getUser().getId().equals(userId) && !user.isAdmin()) {
            throw new ForbiddenException("Bạn không có quyền xóa đánh giá này");
        }

        reviewRepository.delete(review);

        log.info("Review {} deleted by user {}", reviewId, userId);
    }

    private ReviewDTO mapToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .eventId(review.getEvent().getId())
                .eventTitle(review.getEvent().getTitle())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isApproved(review.getIsApproved())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}