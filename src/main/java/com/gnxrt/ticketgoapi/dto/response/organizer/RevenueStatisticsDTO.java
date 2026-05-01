package com.gnxrt.ticketgoapi.dto.response.organizer;

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
public class RevenueStatisticsDTO {

    private String period;
    private String from;
    private String to;
    private BigDecimal totalRevenue;
    private Long totalTicketsSold;
    private List<DailyPoint> series;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPoint {
        private String date;
        private BigDecimal revenue;
        private Long ticketsSold;
    }
}
