package com.gnxrt.ticketgoapi.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReviewSummaryDTO {

    private Long eventId;
    private Double averageRating;
    private Long totalReviews;

    private Map<Integer, Long> ratingDistribution;

    private List<ReviewDTO> recentReviews;

    private ReviewDTO userReview;

    private Boolean canReview;
    private String cannotReviewReason;
}