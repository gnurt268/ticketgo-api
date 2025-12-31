package com.gnxrt.ticketgoapi.dto.response.seat;

import com.gnxrt.ticketgoapi.enums.SeatStatus;
import com.gnxrt.ticketgoapi.enums.SeatType;
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
public class SeatDTO {

    private Long id;

    private Long ticketZoneId;

    private String zoneName;

    private String rowLabel;

    private Integer seatNumber;

    private String seatCode;

    private Integer positionX;

    private Integer positionY;

    private SeatStatus status;

    private BigDecimal price;

    private SeatType seatType;

    private Boolean isAvailable;

    private Boolean isReservedByCurrentUser;

    private LocalDateTime reservedUntil;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}