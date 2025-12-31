package com.gnxrt.ticketgoapi.dto.response.event;

import com.gnxrt.ticketgoapi.enums.EventStatus;
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
public class EventDetailDTO {

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

    private EventStatus status;

    private EventType eventType;

    private Boolean isFeatured;

    private Integer maxTicketsPerOrder;

    private Boolean enableSeatSelection;

    private String seatMapImageUrl;

    private Boolean enableFaceRecognition;

    private BigDecimal faceRecognitionThreshold;

    private Boolean requireFaceUpload;

    private Integer viewCount;

    private Integer totalTicketsSold;

    private BigDecimal totalRevenue;

    // Organizer info
    private Long organizerId;
    private String organizerName;
    private String organizerEmail;
    private String organizerPhone;

    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    private Integer totalTicketZones;
    private Integer totalOrders;
    private Integer totalReviews;
    private Double averageRating;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}