package com.gnxrt.ticketgoapi.dto.response.seat;

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
public class SeatReservationDTO {

    private String reservationId;

    private Long eventId;

    private String eventTitle;

    private Long ticketZoneId;

    private String zoneName;

    private List<SeatDTO> reservedSeats;

    private Integer totalSeats;

    private BigDecimal totalPrice;

    private String currency;

    private LocalDateTime reservedAt;

    private LocalDateTime expiresAt;

    private Integer remainingSeconds;

    private String status;

    private String message;
}