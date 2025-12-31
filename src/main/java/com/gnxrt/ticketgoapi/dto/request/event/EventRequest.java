package com.gnxrt.ticketgoapi.dto.request.event;

import com.gnxrt.ticketgoapi.enums.EventType;
import jakarta.validation.constraints.*;
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
public class EventRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    private String posterUrl;

    @Size(max = 500, message = "Banner URL must not exceed 500 characters")
    private String bannerUrl;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @NotBlank(message = "Venue is required")
    @Size(max = 255, message = "Venue must not exceed 255 characters")
    private String venue;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @Builder.Default
    private Boolean isFeatured = false;

    @Min(value = 1, message = "Max tickets per order must be at least 1")
    @Max(value = 50, message = "Max tickets per order must not exceed 50")
    @Builder.Default
    private Integer maxTicketsPerOrder = 10;

    @Builder.Default
    private Boolean enableSeatSelection = false;

    @Size(max = 500, message = "Seat map image URL must not exceed 500 characters")
    private String seatMapImageUrl;

    @Builder.Default
    private Boolean enableFaceRecognition = true;

    @DecimalMin(value = "0.50", message = "Face recognition threshold must be at least 0.50")
    @DecimalMax(value = "1.00", message = "Face recognition threshold must not exceed 1.00")
    @Builder.Default
    private BigDecimal faceRecognitionThreshold = new BigDecimal("0.70");

    @Builder.Default
    private Boolean requireFaceUpload = true;

    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}