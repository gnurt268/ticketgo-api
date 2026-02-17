package com.gnxrt.ticketgoapi.dto.response.user;

import com.gnxrt.ticketgoapi.enums.Role;
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
public class UserProfileDTO {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Role role;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thống kê
    private Integer totalOrders;
    private Integer totalTickets;
    private BigDecimal totalSpent;
    private Integer totalReviews;
    private Integer totalEventsOrganized;
}