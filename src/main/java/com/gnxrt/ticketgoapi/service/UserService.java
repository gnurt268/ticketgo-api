package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.user.ChangePasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.user.UserProfileUpdateRequest;
import com.gnxrt.ticketgoapi.dto.response.user.UserProfileDTO;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy profile của user đang đăng nhập
     */
    public UserProfileDTO getMyProfile() {
        User user = getCurrentUser();
        return mapToProfileDTO(user);
    }

    /**
     * Cập nhật profile
     */
    @Transactional
    public UserProfileDTO updateMyProfile(UserProfileUpdateRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            User finalUser = user;
            userRepository.findByPhone(request.getPhone())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(finalUser.getId())) {
                            throw new BadRequestException("Số điện thoại đã được sử dụng bởi tài khoản khác");
                        }
                    });
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("User {} updated profile successfully", user.getId());

        return mapToProfileDTO(user);
    }

    /**
     * Đổi mật khẩu
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // Validate mật khẩu mới và xác nhận khớp nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Validate mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }

        // Validate mật khẩu mới khác mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("User {} changed password successfully", user.getId());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private UserProfileDTO mapToProfileDTO(User user) {
        Integer totalOrders = userRepository.countOrdersByUserId(user.getId());
        Integer totalTickets = userRepository.countTicketsPurchasedByUserId(user.getId());
        BigDecimal totalSpent = userRepository.getTotalSpentByUserId(user.getId());
        Integer totalReviews = userRepository.countReviewsByUserId(user.getId());
        Integer totalEvents = userRepository.countEventsOrganizedByUserId(user.getId());

        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalOrders(totalOrders)
                .totalTickets(totalTickets)
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .totalReviews(totalReviews)
                .totalEventsOrganized(totalEvents)
                .build();
    }
}