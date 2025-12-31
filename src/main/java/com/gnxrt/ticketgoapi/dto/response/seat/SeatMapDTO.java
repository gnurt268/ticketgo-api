package com.gnxrt.ticketgoapi.dto.response.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapDTO {

    private Long eventId;

    private String eventTitle;

    private Long ticketZoneId;

    private String zoneName;

    private String zoneCode;

    private BigDecimal zonePrice;

    private Integer totalRows;

    private Integer maxSeatsPerRow;

    private Integer totalSeats;

    private Integer availableSeats;

    private Integer reservedSeats;

    private Integer soldSeats;

    private Integer blockedSeats;

    private Map<String, List<SeatDTO>> seatsByRow;

    private List<SeatDTO> allSeats;

    private List<SeatLegend> legend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatLegend {
        private String status;
        private String label;
        private String color;
    }
}