package com.gnxrt.ticketgoapi.dto.response.organizer;

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
public class OrganizerDashboardDTO {

    private long totalEvents;
    private long totalTicketsSold;
    private BigDecimal totalRevenue;
    private long totalCheckIns;
    private Map<String, Long> eventsByStatus;

    private List<RevenueByDay> revenueChart;
    private List<TicketSalesByDay> ticketSalesChart;
    private List<TopEventDTO> topEventsByRevenue;
    private List<CheckInRateDTO> checkInRates;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RevenueByDay {
        private String date;
        private BigDecimal revenue;
        private long orderCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TicketSalesByDay {
        private String date;
        private long ticketsSold;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopEventDTO {
        private Long eventId;
        private String eventTitle;
        private BigDecimal revenue;
        private long ticketsSold;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CheckInRateDTO {
        private Long eventId;
        private String eventTitle;
        private long totalTickets;
        private long checkedIn;
        private double checkInRate;
    }
}
