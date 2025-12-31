package com.gnxrt.ticketgoapi.dto.request.ticketzone;

import com.gnxrt.ticketgoapi.enums.ZoneType;
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
public class TicketZoneRequest {

    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Zone name must not exceed 100 characters")
    private String zoneName;

    @NotBlank(message = "Zone code is required")
    @Size(max = 50, message = "Zone code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Zone code must contain only uppercase letters, numbers, underscores and hyphens")
    private String zoneCode;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 7, message = "Color code must be valid hex color (e.g., #3B82F6)")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color code must be valid hex format")
    @Builder.Default
    private String colorCode = "#3B82F6";

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be greater than or equal to 0")
    @Digits(integer = 13, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @Builder.Default
    private String currency = "VND";

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Total capacity must be at least 1")
    @Max(value = 100000, message = "Total capacity must not exceed 100,000")
    private Integer totalCapacity;

    @Builder.Default
    private ZoneType zoneType = ZoneType.STANDARD;

    @Builder.Default
    private Boolean isActive = true;

    private Integer displayOrder;

    private LocalDateTime saleStartDate;

    private LocalDateTime saleEndDate;

    @AssertTrue(message = "Sale end date must be after sale start date")
    public boolean isValidSaleDateRange() {
        if (saleStartDate == null || saleEndDate == null) {
            return true;
        }
        return saleEndDate.isAfter(saleStartDate);
    }
}