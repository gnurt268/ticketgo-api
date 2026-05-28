package com.gnxrt.ticketgoapi.dto.response.waitingroom;

import com.gnxrt.ticketgoapi.enums.WaitingRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingRoomResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String eventSlug;

    private LocalDateTime preQueueStart;
    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;

    private Integer throughputPerMinute;
    private Integer maxConcurrentUsers;
    private Integer sessionTimeoutMinutes;

    private WaitingRoomStatus status;
    private Boolean isEnabled;
    private LocalDateTime shuffledAt;

    private Integer totalQueueEntries;
    private Integer totalAdmitted;
    private Integer totalCompleted;
    private Integer currentlyWaiting;
    private Integer currentlyShopping;

    private Long secondsUntilPreQueue;
    private Long secondsUntilSaleStart;
    private Boolean canJoinNow;
    private Boolean captchaRequired;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}