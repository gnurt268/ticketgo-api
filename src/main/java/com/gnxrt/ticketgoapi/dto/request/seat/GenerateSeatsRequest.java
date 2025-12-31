package com.gnxrt.ticketgoapi.dto.request.seat;

import com.gnxrt.ticketgoapi.enums.SeatType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSeatsRequest {

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Number of rows must be at least 1")
    @Max(value = 52, message = "Number of rows must not exceed 52 (A-Z, AA-AZ)")
    private Integer rows;

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "Seats per row must be at least 1")
    @Max(value = 100, message = "Seats per row must not exceed 100")
    private Integer seatsPerRow;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", message = "Base price must be greater than or equal to 0")
    private BigDecimal basePrice;

    @Builder.Default
    private SeatType defaultSeatType = SeatType.STANDARD;

    private List<Integer> aislePositions;

    private List<String> vipRows;

    private BigDecimal vipPriceMultiplier;

    private List<String> blockedSeats;
}