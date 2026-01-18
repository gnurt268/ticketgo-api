package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.QueueEntryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries", indexes = {
        @Index(name = "idx_qe_waiting_room", columnList = "waiting_room_id"),
        @Index(name = "idx_qe_user", columnList = "user_id"),
        @Index(name = "idx_qe_status", columnList = "status"),
        @Index(name = "idx_qe_position", columnList = "waiting_room_id, queue_position"),
        @Index(name = "idx_qe_visitor_token", columnList = "visitor_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiting_room_id", nullable = false)
    private WaitingRoom waitingRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "visitor_token", nullable = false, unique = true, length = 64)
    private String visitorToken;

    @Column(name = "access_token", length = 64)
    private String accessToken;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QueueEntryStatus status = QueueEntryStatus.WAITING;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "fingerprint", length = 500)
    private String fingerprint;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isWaiting() {
        return status == QueueEntryStatus.WAITING;
    }

    public boolean isReady() {
        return status == QueueEntryStatus.READY;
    }

    public boolean isShopping() {
        return status == QueueEntryStatus.SHOPPING;
    }

    public boolean isActive() {
        return status == QueueEntryStatus.WAITING
                || status == QueueEntryStatus.READY
                || status == QueueEntryStatus.SHOPPING;
    }

    public boolean isSessionExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}