package com.gnxrt.ticketgoapi.dto.response.user;

import com.gnxrt.ticketgoapi.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListDTO {

    private Long id;

    private String email;

    private String fullName;

    private String phone;

    private String avatarUrl;

    private Role role;

    private Boolean isActive;

    private Boolean emailVerified;

    private Integer totalOrders;

    private Integer totalEventsOrganized;

    private LocalDateTime createdAt;
}