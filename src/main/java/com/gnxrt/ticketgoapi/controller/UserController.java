package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.user.ChangePasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.user.DeleteAccountRequest;
import com.gnxrt.ticketgoapi.dto.request.user.UserPreferencesRequest;
import com.gnxrt.ticketgoapi.dto.request.user.UserProfileUpdateRequest;
import com.gnxrt.ticketgoapi.dto.response.user.UserPreferencesDTO;
import com.gnxrt.ticketgoapi.dto.response.user.UserProfileDTO;
import com.gnxrt.ticketgoapi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * POST /api/users/avatar - Upload avatar lên Cloudinary và cập nhật profile
     */
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileDTO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        UserProfileDTO profile = userService.uploadAvatar(file);
        return ResponseEntity.ok(profile);
    }

    /**
     * DELETE /api/users/profile - Soft-delete tài khoản
     */
    @DeleteMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount(
            HttpServletRequest httpRequest,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
        userService.deleteAccount(request, accessToken);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/users/preferences - Lấy preferences (default nếu chưa có record)
     */
    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesDTO> getMyPreferences() {
        return ResponseEntity.ok(userService.getMyPreferences());
    }

    /**
     * PATCH /api/users/preferences - Cập nhật preferences (null = không đổi)
     */
    @PatchMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesDTO> updateMyPreferences(
            @Valid @RequestBody UserPreferencesRequest request
    ) {
        return ResponseEntity.ok(userService.updateMyPreferences(request));
    }
}