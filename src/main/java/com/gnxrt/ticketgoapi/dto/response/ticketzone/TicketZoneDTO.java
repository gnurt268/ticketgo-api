package com.gnxrt.ticketgoapi.dto.response.ticketzone;

import com.gnxrt.ticketgoapi.enums.ZoneType;
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
public class TicketZoneDTO {

    private Long id;

    private Long eventId;

    private String eventTitle;

    private String zoneName;

    private String zoneCode;

    private String description;

    private String colorCode;

    private BigDecimal price;

    private String currency;

    private Integer totalCapacity;

    private Integer availableCapacity;

    private Integer reservedCapacity;

    private Integer soldCapacity;

    private ZoneType zoneType;

    private Boolean isActive;

    private Integer displayOrder;

    private LocalDateTime saleStartDate;

    private LocalDateTime saleEndDate;

    private Boolean isSaleActive;

    private Boolean isSoldOut;

    private Double soldPercentage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}