package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.user.UserUpdateRequest;
import com.gnxrt.ticketgoapi.dto.response.user.UserDetailDTO;
import com.gnxrt.ticketgoapi.dto.response.user.UserListDTO;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    public Page<UserListDTO> getAllUsers(Pageable pageable) {
        log.info("Getting all users with pagination");
        return userRepository.findAll(pageable)
                .map(this::mapToListDTO);
    }

    public Page<UserListDTO> getUsersByRole(Role role, Pageable pageable) {
        log.info("Getting users by role: {}", role);
        return userRepository.findByRole(role, pageable)
                .map(this::mapToListDTO);
    }

    public Page<UserListDTO> getUsersByActiveStatus(Boolean isActive, Pageable pageable) {
        log.info("Getting users by active status: {}", isActive);
        return userRepository.findByIsActive(isActive, pageable)
                .map(this::mapToListDTO);
    }

    public Page<UserListDTO> searchUsers(String keyword, Pageable pageable) {
        log.info("Searching users with keyword: {}", keyword);
        return userRepository.searchUsers(keyword, pageable)
                .map(this::mapToListDTO);
    }

    public Page<UserListDTO> getUsersByRoleAndActiveStatus(
            Role role,
            Boolean isActive,
            Pageable pageable
    ) {
        log.info("Getting users by role: {} and active status: {}", role, isActive);
        return userRepository.findByRoleAndIsActive(role, isActive, pageable)
                .map(this::mapToListDTO);
    }

    public UserDetailDTO getUserDetail(Long userId) {
        log.info("Getting user detail for id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToDetailDTO(user);
    }

    @Transactional
    public UserDetailDTO updateUser(Long userId, UserUpdateRequest request) {
        log.info("Updating user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }

        user = userRepository.save(user);
        log.info("User updated successfully with id: {}", user.getId());

        return mapToDetailDTO(user);
    }

    @Transactional
    public UserDetailDTO toggleActive(Long userId) {
        log.info("Toggling active status for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);

        log.info("User active status toggled to: {} for id: {}", user.getIsActive(), userId);
        return mapToDetailDTO(user);
    }

    @Transactional
    public UserDetailDTO changeUserRole(Long userId, Role newRole) {
        log.info("Changing role for user id: {} to: {}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setRole(newRole);
        user = userRepository.save(user);

        log.info("User role changed successfully for id: {}", userId);
        return mapToDetailDTO(user);
    }

    @Transactional
    public UserDetailDTO verifyEmail(Long userId) {
        log.info("Verifying email for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEmailVerified(true);
        user = userRepository.save(user);

        log.info("Email verified successfully for user id: {}", userId);
        return mapToDetailDTO(user);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated successfully with id: {}", userId);
    }

    @Transactional
    public void deleteUserPermanently(Long userId) {
        log.info("Permanently deleting user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Integer orderCount = userRepository.countOrdersByUserId(userId);
        if (orderCount > 0) {
            throw new BadRequestException("Không thể xóa người dùng có " + orderCount + " đơn hàng");
        }

        Integer eventCount = userRepository.countEventsOrganizedByUserId(userId);
        if (eventCount > 0) {
            throw new BadRequestException("Không thể xóa người dùng đang tổ chức " + eventCount + " sự kiện");
        }

        userRepository.delete(user);
        log.info("User permanently deleted with id: {}", userId);
    }

    public List<UserListDTO> getUsersCreatedBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        log.info("Getting users created between {} and {}", startDate, endDate);
        return userRepository.findUsersCreatedBetween(startDate, endDate).stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    public Long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public Long countActiveUsers() {
        return userRepository.countByIsActive(true);
    }

    private UserListDTO mapToListDTO(User user) {
        Integer totalOrders = userRepository.countOrdersByUserId(user.getId());
        Integer totalEvents = userRepository.countEventsOrganizedByUserId(user.getId());

        return UserListDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .totalOrders(totalOrders)
                .totalEventsOrganized(totalEvents)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserDetailDTO mapToDetailDTO(User user) {
        Integer totalOrders = userRepository.countOrdersByUserId(user.getId());
        Integer totalTickets = userRepository.countTicketsPurchasedByUserId(user.getId());
        BigDecimal totalSpent = userRepository.getTotalSpentByUserId(user.getId());
        Integer totalEvents = userRepository.countEventsOrganizedByUserId(user.getId());
        Integer totalReviews = userRepository.countReviewsByUserId(user.getId());
        Integer totalInteractions = userRepository.countInteractionsByUserId(user.getId());

        return UserDetailDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalOrders(totalOrders)
                .totalTicketsPurchased(totalTickets)
                .totalSpent(totalSpent)
                .totalEventsOrganized(totalEvents)
                .totalReviews(totalReviews)
                .totalInteractions(totalInteractions)
                .lastLoginAt(null) // TODO: Implement login tracking
                .build();
    }
}