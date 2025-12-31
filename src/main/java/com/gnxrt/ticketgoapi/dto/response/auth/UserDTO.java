package com.gnxrt.ticketgoapi.dto.response.auth;

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
public class UserDTO {

    private Long id;

    private String email;

    private String fullName;

    private String phone;

    private String avatarUrl;

    private Role role;

    private Boolean isActive;

    private Boolean emailVerified;

    private LocalDateTime createdAt;
}