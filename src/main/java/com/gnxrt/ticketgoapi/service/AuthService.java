package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.auth.ForgotPasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.LoginRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.RegisterRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.ResetPasswordRequest;
import com.gnxrt.ticketgoapi.dto.response.auth.AuthResponse;
import com.gnxrt.ticketgoapi.dto.response.auth.UserDTO;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.exception.UnauthorizedException;
import com.gnxrt.ticketgoapi.model.PasswordResetToken;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.PasswordResetTokenRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import com.gnxrt.ticketgoapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration RESEND_VERIFY_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration FORGOT_PASSWORD_COOLDOWN = Duration.ofSeconds(60);
    private static final String RESEND_VERIFY_KEY_PREFIX = "rl:resend-verify:";
    private static final String FORGOT_PASSWORD_KEY_PREFIX = "rl:forgot-password:";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã được sử dụng");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.USER)
                .isActive(true)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);

        emailService.sendWelcomeEmail(user);
        emailService.sendVerificationEmail(user, verificationToken);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDTO(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(mapToUserDTO(user))
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.info("Processing refresh token request");

        try {
            String jti = jwtService.extractJti(refreshToken);
            if (tokenBlacklistService.isBlacklisted(jti)) {
                throw new UnauthorizedException("Refresh token has been revoked");
            }

            String email = jwtService.extractUsername(refreshToken);

            if (email == null) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            if (!user.getIsActive()) {
                throw new UnauthorizedException("User account is disabled");
            }

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            String newAccessToken = jwtService.generateToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("Token refreshed successfully for user: {}", email);

            return AuthResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .user(mapToUserDTO(user))
                    .build();

        } catch (UnauthorizedException e) {
            log.warn("Refresh token rejected: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }

    @Transactional
    public UserDTO verifyEmail(String token) {
        log.info("Verifying email with token");

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Token xác thực không hợp lệ"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực");
        }

        if (user.getVerificationTokenExpiresAt() == null
                || user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token xác thực đã hết hạn");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified successfully for user id: {}", user.getId());
        return mapToUserDTO(user);
    }

    @Transactional
    public void resendVerificationEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        String email = authentication.getName();
        log.info("Resending verification email for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực");
        }

        String rateLimitKey = RESEND_VERIFY_KEY_PREFIX + user.getId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, "1", RESEND_VERIFY_COOLDOWN);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BadRequestException("Vui lòng đợi trước khi yêu cầu gửi lại email xác thực");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(user, verificationToken);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        log.info("Processing forgot-password request for: {}", email);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Forgot-password requested for non-existing email: {}", email);
            return;
        }

        String rateLimitKey = FORGOT_PASSWORD_KEY_PREFIX + user.getId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, "1", FORGOT_PASSWORD_COOLDOWN);
        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("Forgot-password rate limited for user id: {}", user.getId());
            return;
        }

        passwordResetTokenRepository.deleteUnusedByUserId(user.getId());

        String rawToken = generateResetToken();
        String tokenHash = sha256Hex(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plus(RESET_TOKEN_TTL))
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user, rawToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        String tokenHash = sha256Hex(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Token đặt lại mật khẩu không hợp lệ"));

        if (resetToken.getUsedAt() != null) {
            throw new BadRequestException("Token đặt lại mật khẩu đã được sử dụng");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token đặt lại mật khẩu đã hết hạn");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user id: {}", user.getId());
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Logout: blacklist access token + refresh token (nếu có) qua Redis.
     * Blacklist đến khi token expire (Redis tự cleanup theo TTL).
     */
    public void logout(String accessToken, String refreshToken) {
        blacklistToken(accessToken, "access");
        if (refreshToken != null && !refreshToken.isBlank()) {
            blacklistToken(refreshToken, "refresh");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : "unknown";
        log.info("User logged out: {}", email);

        SecurityContextHolder.clearContext();
    }

    private void blacklistToken(String token, String type) {
        try {
            String jti = jwtService.extractJti(token);
            Date expiration = jwtService.extractExpiration(token);
            if (jti == null || expiration == null) {
                return;
            }
            long millisLeft = expiration.getTime() - System.currentTimeMillis();
            if (millisLeft <= 0) {
                return;
            }
            tokenBlacklistService.blacklist(jti, Duration.ofMillis(millisLeft));
        } catch (Exception e) {
            log.warn("Failed to blacklist {} token: {}", type, e.getMessage());
        }
    }

    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return mapToUserDTO(user);
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}