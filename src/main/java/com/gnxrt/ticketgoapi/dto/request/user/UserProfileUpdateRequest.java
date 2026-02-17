package com.gnxrt.ticketgoapi.dto.request.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    @Size(min = 2, max = 255, message = "Họ tên phải từ 2 đến 255 ký tự")
    private String fullName;

    @Pattern(regexp = "^(\\+?84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 500, message = "Avatar URL không được vượt quá 500 ký tự")
    private String avatarUrl;
}