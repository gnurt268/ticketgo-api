package com.gnxrt.ticketgoapi.dto.response.waitingroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinQueueResponse {

    private Boolean success;
    private String message;

    private String visitorToken;

    private Long waitingRoomId;
    private Long eventId;
    private String eventTitle;

    private LocalDateTime joinedAt;
    private LocalDateTime saleStart;
    private Long secondsUntilSaleStart;

    private Integer totalInQueue;

    private Boolean isReconnect;

    public static JoinQueueResponse success(
            String visitorToken, Long waitingRoomId, Long eventId,
            String eventTitle, LocalDateTime joinedAt, LocalDateTime saleStart,
            int totalInQueue, boolean isReconnect) {

        long secondsUntil = java.time.Duration.between(LocalDateTime.now(), saleStart).getSeconds();

        return JoinQueueResponse.builder()
                .success(true)
                .message(isReconnect ? "Chào mừng trở lại! Bạn vẫn giữ được vị trí trong phòng chờ."
                        : "Bạn đã vào phòng chờ thành công!")
                .visitorToken(visitorToken)
                .waitingRoomId(waitingRoomId)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .joinedAt(joinedAt)
                .saleStart(saleStart)
                .secondsUntilSaleStart(Math.max(0, secondsUntil))
                .totalInQueue(totalInQueue)
                .isReconnect(isReconnect)
                .build();
    }

    public static JoinQueueResponse failure(String message) {
        return JoinQueueResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}