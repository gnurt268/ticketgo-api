package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.DeviceType;
import com.gnxrt.ticketgoapi.enums.InteractionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * UserEventInteraction Entity
 */
@Entity
@Table(name = "user_event_interactions", indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_event", columnList = "event_id"),
        @Index(name = "idx_interaction_type", columnList = "interaction_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private InteractionType interactionType;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "referrer_url", columnDefinition = "TEXT")
    private String referrerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    private DeviceType deviceType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}

