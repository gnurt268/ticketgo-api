package com.gnxrt.ticketgoapi.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueUpdateMessage {
    public enum Event { SELLING_STARTED, POSITION_UPDATE, READY, EXPIRED }

    private Event event;
    private Long eventId;
    private Long userId;
    private Integer position;
    private Integer totalInQueue;
    private String accessToken;
    private String message;
    private LocalDateTime timestamp;
}
