package com.gnxrt.ticketgoapi.dto.response.eventstaff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStaffDTO {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private LocalDateTime assignedAt;
}
