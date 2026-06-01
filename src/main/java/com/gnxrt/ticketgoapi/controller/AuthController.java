package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.auth.ForgotPasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.LoginRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.LogoutRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.RefreshTokenRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.RegisterRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.ResetPasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.VerifyEmailRequest;
import com.gnxrt.ticketgoapi.dto.response.auth.AuthResponse;
import com.gnxrt.ticketgoapi.dto.response.auth.UserDTO;
import com.gnxrt.ticketgoapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest request
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser() {
        UserDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<UserDTO> verifyEmail(@RequestParam("token") String token) {
        UserDTO user = authService.verifyEmail(token);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<UserDTO> verifyEmailPost(@Valid @RequestBody VerifyEmailRequest request) {
        UserDTO user = authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resendVerification() {
        authService.resendVerificationEmail();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
