package com.gnxrt.ticketgoapi.dto.response.event;

import com.gnxrt.ticketgoapi.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicEventDetailDTO {

    private Long id;

    private String title;

    private String slug;

    private String description;

    private String posterUrl;

    private String bannerUrl;

    private String location;

    private String venue;

    private String address;

    private String city;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private EventType eventType;

    private Boolean isFeatured;

    private Integer maxTicketsPerOrder;

    private Boolean enableSeatSelection;

    private String seatMapImageUrl;

    private Boolean enableFaceRecognition;

    private Boolean requireFaceUpload;

    private Integer viewCount;

    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    private Long organizerId;
    private String organizerName;
    private String organizerAvatarUrl;

    private List<TicketZoneDTO> ticketZones;

    private Double averageRating;
    private Integer totalReviews;
    private List<ReviewSummaryDTO> recentReviews;

    private Integer totalCapacity;
    private Integer availableCapacity;
    private Boolean isSoldOut;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketZoneDTO {
        private Long id;
        private String zoneName;
        private String zoneCode;
        private String description;
        private String colorCode;
        private BigDecimal price;
        private String currency;
        private Integer totalCapacity;
        private Integer availableCapacity;
        private String zoneType;
        private Boolean isActive;
        private Integer displayOrder;
        private LocalDateTime saleStartDate;
        private LocalDateTime saleEndDate;
        private Boolean isSaleActive;
        private Boolean isSoldOut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummaryDTO {
        private Long id;
        private String userName;
        private String userAvatarUrl;
        private Integer rating;
        private String title;
        private String comment;
        private String sentimentLabel;
        private LocalDateTime createdAt;
    }
}