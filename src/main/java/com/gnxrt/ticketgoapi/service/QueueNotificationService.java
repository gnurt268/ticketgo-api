package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.event.QueueUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public static String userTopic(Long eventId, Long userId) {
        return "/topic/queue/event/" + eventId + "/user/" + userId;
    }

    public static String eventTopic(Long eventId) {
        return "/topic/queue/event/" + eventId;
    }

    public void notifyUserReady(Long eventId, Long userId, String accessToken) {
        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .event(QueueUpdateMessage.Event.READY)
                .eventId(eventId)
                .userId(userId)
                .accessToken(accessToken)
                .message("Bạn đã được phép vào mua vé")
                .timestamp(LocalDateTime.now())
                .build();
        sendSafe(userTopic(eventId, userId), msg);
    }

    public void notifyUserExpired(Long eventId, Long userId) {
        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .event(QueueUpdateMessage.Event.EXPIRED)
                .eventId(eventId)
                .userId(userId)
                .message("Phiên mua vé đã hết hạn")
                .timestamp(LocalDateTime.now())
                .build();
        sendSafe(userTopic(eventId, userId), msg);
    }

    public void notifySellingStarted(Long eventId, int totalInQueue) {
        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .event(QueueUpdateMessage.Event.SELLING_STARTED)
                .eventId(eventId)
                .totalInQueue(totalInQueue)
                .message("Mở bán vé")
                .timestamp(LocalDateTime.now())
                .build();
        sendSafe(eventTopic(eventId), msg);
    }

    public void notifyPositionUpdate(Long eventId, Long userId, int position, int totalInQueue) {
        QueueUpdateMessage msg = QueueUpdateMessage.builder()
                .event(QueueUpdateMessage.Event.POSITION_UPDATE)
                .eventId(eventId)
                .userId(userId)
                .position(position)
                .totalInQueue(totalInQueue)
                .timestamp(LocalDateTime.now())
                .build();
        sendSafe(userTopic(eventId, userId), msg);
    }

    private void sendSafe(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.warn("Failed to publish WS message to {}: {}", destination, ex.getMessage());
        }
    }
}
