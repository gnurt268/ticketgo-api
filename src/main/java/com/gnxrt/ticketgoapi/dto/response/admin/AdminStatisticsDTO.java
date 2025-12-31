package com.gnxrt.ticketgoapi.dto.response.admin;

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
public class AdminStatisticsDTO {

    private UserStats userStats;

    private EventStats eventStats;

    private RevenueStats revenueStats;

    private TicketStats ticketStats;

    private RecentActivities recentActivities;

    private TrendData trendData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long totalUsers;
        private Long totalAdmins;
        private Long totalOrganizers;
        private Long totalRegularUsers;
        private Long activeUsers;
        private Long inactiveUsers;
        private Long newUsersToday;
        private Long newUsersThisWeek;
        private Long newUsersThisMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventStats {
        private Long totalEvents;
        private Long draftEvents;
        private Long pendingEvents;
        private Long approvedEvents;
        private Long publishedEvents;
        private Long cancelledEvents;
        private Long completedEvents;
        private Long featuredEvents;
        private Long eventsThisMonth;
        private Long upcomingEvents;
        private Long ongoingEvents;
        private Long pastEvents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private BigDecimal totalRevenue;
        private BigDecimal revenueToday;
        private BigDecimal revenueThisWeek;
        private BigDecimal revenueThisMonth;
        private BigDecimal revenueThisYear;
        private Long totalOrders;
        private Long pendingOrders;
        private Long paidOrders;
        private Long failedOrders;
        private Long cancelledOrders;
        private BigDecimal averageOrderValue;
        private BigDecimal topEventRevenue;
        private String topEventName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketStats {
        private Long totalTicketsSold;
        private Long totalTicketsCheckedIn;
        private Long ticketsSoldToday;
        private Long ticketsSoldThisWeek;
        private Long ticketsSoldThisMonth;
        private Double checkInRate;
        private Long faceRecognitionCheckIns;
        private Long qrCodeCheckIns;
        private Long manualCheckIns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivities {
        private List<RecentUser> recentUsers;
        private List<RecentEvent> recentEvents;
        private List<RecentOrder> recentOrders;
        private List<PendingApproval> pendingApprovals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentUser {
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentEvent {
        private Long id;
        private String title;
        private String organizerName;
        private String status;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrder {
        private Long id;
        private String orderCode;
        private String buyerName;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingApproval {
        private Long id;
        private String title;
        private String organizerName;
        private String submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private List<DataPoint> userRegistrationTrend;
        private List<DataPoint> revenueTrend;
        private List<DataPoint> ticketSalesTrend;
        private Map<String, Long> eventsByCategory;
        private Map<String, Long> eventsByCity;
        private Map<String, BigDecimal> revenueByCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String date;
        private Long value;
        private BigDecimal amount;
    }
}