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
public class ReviewDTO {

    private Long id;

    private Long userId;
    private String userName;
    private String userAvatarUrl;

    private Long eventId;
    private String eventTitle;

    private Integer rating;
    private String title;
    private String comment;

    private Boolean isApproved;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}