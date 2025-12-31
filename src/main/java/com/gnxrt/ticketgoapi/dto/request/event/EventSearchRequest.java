package com.gnxrt.ticketgoapi.dto.request.event;

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
public class EventSearchRequest {

    private String keyword;

    private Long categoryId;

    private String city;

    private EventType eventType;

    private LocalDateTime startDateFrom;

    private LocalDateTime startDateTo;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private Boolean isFeatured;

    @Builder.Default
    private String sortBy = "startDate";

    @Builder.Default
    private String sortDirection = "asc";
}