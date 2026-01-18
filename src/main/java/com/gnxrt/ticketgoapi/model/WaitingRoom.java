package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.WaitingRoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "waiting_rooms", indexes = {
        @Index(name = "idx_wr_event", columnList = "event_id"),
        @Index(name = "idx_wr_status", columnList = "status"),
        @Index(name = "idx_wr_sale_start", columnList = "sale_start")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "pre_queue_start", nullable = false)
    private LocalDateTime preQueueStart;

    @Column(name = "sale_start", nullable = false)
    private LocalDateTime saleStart;

    @Column(name = "sale_end")
    private LocalDateTime saleEnd;

    @Column(name = "throughput_per_minute", nullable = false)
    @Builder.Default
    private Integer throughputPerMinute = 500;

    @Column(name = "max_concurrent_users", nullable = false)
    @Builder.Default
    private Integer maxConcurrentUsers = 1000;

    @Column(name = "session_timeout_minutes", nullable = false)
    @Builder.Default
    private Integer sessionTimeoutMinutes = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WaitingRoomStatus status = WaitingRoomStatus.SCHEDULED;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "shuffled_at")
    private LocalDateTime shuffledAt;

    @Column(name = "total_queue_entries", nullable = false)
    @Builder.Default
    private Integer totalQueueEntries = 0;

    @Column(name = "total_admitted", nullable = false)
    @Builder.Default
    private Integer totalAdmitted = 0;

    @Column(name = "total_completed", nullable = false)
    @Builder.Default
    private Integer totalCompleted = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isPreQueueOpen() {
        LocalDateTime now = LocalDateTime.now();
        return isEnabled
                && status == WaitingRoomStatus.PRE_QUEUE
                && now.isAfter(preQueueStart)
                && now.isBefore(saleStart);
    }

    public boolean isSelling() {
        return isEnabled && status == WaitingRoomStatus.SELLING;
    }

    public boolean canJoinPreQueue() {
        LocalDateTime now = LocalDateTime.now();
        return isEnabled
                && (status == WaitingRoomStatus.SCHEDULED || status == WaitingRoomStatus.PRE_QUEUE)
                && now.isAfter(preQueueStart)
                && now.isBefore(saleStart);
    }

    public boolean shouldStartPreQueue() {
        LocalDateTime now = LocalDateTime.now();
        return isEnabled
                && status == WaitingRoomStatus.SCHEDULED
                && now.isAfter(preQueueStart);
    }

    public boolean shouldStartSelling() {
        LocalDateTime now = LocalDateTime.now();
        return isEnabled
                && status == WaitingRoomStatus.PRE_QUEUE
                && now.isAfter(saleStart);
    }
}