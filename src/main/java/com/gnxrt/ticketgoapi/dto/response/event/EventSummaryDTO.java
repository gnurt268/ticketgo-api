package com.gnxrt.ticketgoapi.dto.response.event;

import com.gnxrt.ticketgoapi.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryDTO {

    private Long id;

    private String title;

    private String slug;

    private String posterUrl;

    private String location;

    private String venue;

    private String city;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private EventType eventType;

    private Boolean isFeatured;

    private Integer viewCount;

    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    private Long organizerId;
    private String organizerName;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;

    private Integer totalCapacity;
    private Integer availableCapacity;
    private Boolean isSoldOut;

    private Double averageRating;
    private Integer totalReviews;
}