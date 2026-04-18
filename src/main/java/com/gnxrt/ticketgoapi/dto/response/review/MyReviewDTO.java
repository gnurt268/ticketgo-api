package com.gnxrt.ticketgoapi.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyReviewDTO {

    private Long id;

    private Integer rating;
    private String title;
    private String comment;

    private Boolean isApproved;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long eventId;
    private String eventTitle;
    private String eventSlug;
    private String eventPosterUrl;
    private LocalDateTime eventStartDate;
}
