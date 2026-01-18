package com.gnxrt.ticketgoapi.dto.response.waitingroom;

import com.gnxrt.ticketgoapi.enums.QueueEntryStatus;
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
public class QueueStatusResponse {

    private Long waitingRoomId;
    private Long eventId;
    private String eventTitle;
    private WaitingRoomStatus roomStatus;

    private String visitorToken;
    private String accessToken;
    private QueueEntryStatus status;
    private Integer queuePosition;
    private LocalDateTime joinedAt;

    private Integer peopleAhead;
    private Integer totalInQueue;
    private Integer estimatedWaitSeconds;

    private Long secondsUntilSaleStart;
    private Long sessionExpiresInSeconds;
    private LocalDateTime expiresAt;

    private String message;
    private String actionUrl;

    private Boolean canEnterNow;
    private Boolean isShuffled;

    public static QueueStatusResponse preQueueStatus(
            Long waitingRoomId, Long eventId, String eventTitle,
            String visitorToken, LocalDateTime joinedAt,
            int totalInQueue, long secondsUntilSale) {

        return QueueStatusResponse.builder()
                .waitingRoomId(waitingRoomId)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .roomStatus(WaitingRoomStatus.PRE_QUEUE)
                .visitorToken(visitorToken)
                .status(QueueEntryStatus.WAITING)
                .joinedAt(joinedAt)
                .totalInQueue(totalInQueue)
                .secondsUntilSaleStart(secondsUntilSale)
                .isShuffled(false)
                .canEnterNow(false)
                .message("Bạn đã vào phòng chờ. Vị trí sẽ được xác định ngẫu nhiên khi mở bán.")
                .build();
    }

    public static QueueStatusResponse waitingInQueue(
            Long waitingRoomId, Long eventId, String eventTitle,
            String visitorToken, int position, int peopleAhead,
            int totalInQueue, int estimatedWaitSeconds) {

        return QueueStatusResponse.builder()
                .waitingRoomId(waitingRoomId)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .roomStatus(WaitingRoomStatus.SELLING)
                .visitorToken(visitorToken)
                .status(QueueEntryStatus.WAITING)
                .queuePosition(position)
                .peopleAhead(peopleAhead)
                .totalInQueue(totalInQueue)
                .estimatedWaitSeconds(estimatedWaitSeconds)
                .isShuffled(true)
                .canEnterNow(false)
                .message(String.format("Vị trí của bạn: #%d. Còn %d người phía trước.", position, peopleAhead))
                .build();
    }

    public static QueueStatusResponse readyToEnter(
            Long waitingRoomId, Long eventId, String eventTitle,
            String visitorToken, String accessToken, String actionUrl,
            int sessionTimeoutMinutes) {

        return QueueStatusResponse.builder()
                .waitingRoomId(waitingRoomId)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .roomStatus(WaitingRoomStatus.SELLING)
                .visitorToken(visitorToken)
                .accessToken(accessToken)
                .status(QueueEntryStatus.READY)
                .isShuffled(true)
                .canEnterNow(true)
                .sessionExpiresInSeconds((long) sessionTimeoutMinutes * 60)
                .actionUrl(actionUrl)
                .message(String.format("Đến lượt bạn! Bạn có %d phút để hoàn tất mua vé.", sessionTimeoutMinutes))
                .build();
    }

    public static QueueStatusResponse shopping(
            Long waitingRoomId, Long eventId, String eventTitle,
            String visitorToken, String accessToken,
            LocalDateTime expiresAt) {

        long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();

        return QueueStatusResponse.builder()
                .waitingRoomId(waitingRoomId)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .roomStatus(WaitingRoomStatus.SELLING)
                .visitorToken(visitorToken)
                .accessToken(accessToken)
                .status(QueueEntryStatus.SHOPPING)
                .isShuffled(true)
                .canEnterNow(true)
                .expiresAt(expiresAt)
                .sessionExpiresInSeconds(Math.max(0, secondsRemaining))
                .message("Bạn đang trong khu vực mua vé. Hoàn tất đặt vé trước khi hết thời gian!")
                .build();
    }
}