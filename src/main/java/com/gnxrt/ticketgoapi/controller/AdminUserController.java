package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.user.UserUpdateRequest;
import com.gnxrt.ticketgoapi.dto.response.user.UserDetailDTO;
import com.gnxrt.ticketgoapi.dto.response.user.UserListDTO;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManagementService userManagementService;

    /**
     * GET /api/admin/users?page=0&size=10&sort=createdAt,desc&role=USER&isActive=true
     */
    @GetMapping
    public ResponseEntity<Page<UserListDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserListDTO> users;

        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userManagementService.searchUsers(keyword, pageable);
        } else if (role != null && isActive != null) {
            users = userManagementService.getUsersByRoleAndActiveStatus(role, isActive, pageable);
        } else if (role != null) {
            users = userManagementService.getUsersByRole(role, pageable);
        } else if (isActive != null) {
            users = userManagementService.getUsersByActiveStatus(isActive, pageable);
        } else {
            users = userManagementService.getAllUsers(pageable);
        }

        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDTO> getUserDetail(@PathVariable Long id) {
        UserDetailDTO user = userManagementService.getUserDetail(id);
        return ResponseEntity.ok(user);
    }

    /**
     * PUT /api/admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDetailDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserDetailDTO user = userManagementService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /api/admin/users/{id}/toggle-active
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<UserDetailDTO> toggleActive(@PathVariable Long id) {
        UserDetailDTO user = userManagementService.toggleActive(id);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /api/admin/users/{id}/change-role
     */
    @PatchMapping("/{id}/change-role")
    public ResponseEntity<UserDetailDTO> changeRole(
            @PathVariable Long id,
            @RequestParam Role role
    ) {
        UserDetailDTO user = userManagementService.changeUserRole(id, role);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /api/admin/users/{id}/verify-email
     */
    @PatchMapping("/{id}/verify-email")
    public ResponseEntity<UserDetailDTO> verifyEmail(@PathVariable Long id) {
        UserDetailDTO user = userManagementService.verifyEmail(id);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE /api/admin/users/{id}/deactivate
     */
    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userManagementService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/admin/users/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deleteUserPermanently(@PathVariable Long id) {
        userManagementService.deleteUserPermanently(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/admin/users/created-between?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GetMapping("/created-between")
    public ResponseEntity<List<UserListDTO>> getUsersCreatedBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<UserListDTO> users = userManagementService.getUsersCreatedBetween(startDate, endDate);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/admin/users/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userManagementService.countActiveUsers());
        stats.put("totalAdmins", userManagementService.countUsersByRole(Role.ADMIN));
        stats.put("totalOrganizers", userManagementService.countUsersByRole(Role.ORGANIZER));
        stats.put("totalRegularUsers", userManagementService.countUsersByRole(Role.USER));

        return ResponseEntity.ok(stats);
    }
}