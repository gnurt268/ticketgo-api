package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.response.admin.AdminStatisticsDTO;
import com.gnxrt.ticketgoapi.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminStatisticsService adminStatisticsService;

    /**
     * GET /api/admin/dashboard/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<AdminStatisticsDTO> getAllStatistics() {
        AdminStatisticsDTO statistics = adminStatisticsService.getAllStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * GET /api/admin/dashboard/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Admin Dashboard is healthy");
    }
}