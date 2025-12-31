package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.auth.LoginRequest;
import com.gnxrt.ticketgoapi.dto.request.auth.RegisterRequest;
import com.gnxrt.ticketgoapi.dto.response.auth.AuthResponse;
import com.gnxrt.ticketgoapi.dto.response.auth.UserDTO;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.exception.UnauthorizedException;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import com.gnxrt.ticketgoapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.USER)
                .isActive(true)
                .emailVerified(false)
                .build();

        userRepository.save(user);

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

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Processing refresh token request");

        try {
            // Extract email from refresh token
            String email = jwtService.extractUsername(refreshToken);

            if (email == null) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            // Find user
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Check if user is active
            if (!user.getIsActive()) {
                throw new UnauthorizedException("User account is disabled");
            }

            // Build UserDetails
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new UnauthorizedException("Invalid or expired refresh token");
            }

            // Generate new tokens
            String newAccessToken = jwtService.generateToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("Token refreshed successfully for user: {}", email);

            return AuthResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .user(mapToUserDTO(user))
                    .build();

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired refresh token");
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