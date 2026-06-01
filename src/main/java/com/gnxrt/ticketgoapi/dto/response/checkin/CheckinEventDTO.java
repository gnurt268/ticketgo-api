package com.gnxrt.ticketgoapi.dto.response.checkin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sự kiện mà người dùng hiện tại được phép check-in (cho màn hình chọn sự kiện).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinEventDTO {
    private Long id;
    private String title;
    private String posterUrl;
    private String venue;
    private String city;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalCheckedIn;
    private Long totalTickets;
}
