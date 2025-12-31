package com.gnxrt.ticketgoapi.dto.request.seat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveSeatsRequest {

    @NotEmpty(message = "Seat IDs are required")
    @Size(max = 10, message = "Cannot reserve more than 10 seats at once")
    private List<Long> seatIds;

    @Builder.Default
    private Integer reservationMinutes = 15;
}