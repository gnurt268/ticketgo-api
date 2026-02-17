package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.user.ChangePasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.user.UserProfileUpdateRequest;
import com.gnxrt.ticketgoapi.dto.response.user.UserProfileDTO;
import com.gnxrt.ticketgoapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/profile - Xem profile của user đang đăng nhập
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        UserProfileDTO profile = userService.getMyProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/users/profile - Cập nhật profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserProfileDTO profile = userService.updateMyProfile(request);
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/users/change-password - Đổi mật khẩu
     */
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}