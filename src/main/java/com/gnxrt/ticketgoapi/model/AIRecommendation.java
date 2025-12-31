package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.Algorithm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AIRecommendation Entity
 */
@Entity
@Table(name = "ai_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_event_rec",
                        columnNames = {"user_id", "event_id", "algorithm"})
        },
        indexes = {
                @Index(name = "idx_user", columnList = "user_id"),
                @Index(name = "idx_event", columnList = "event_id"),
                @Index(name = "idx_score", columnList = "score"),
                @Index(name = "idx_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Algorithm algorithm;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "is_clicked", nullable = false)
    @Builder.Default
    private Boolean isClicked = false;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    public void recordClick() {
        this.isClicked = true;
        this.clickedAt = LocalDateTime.now();
    }
}
