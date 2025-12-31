package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.SentimentLabel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Review Entity
 */
@Entity
@Table(name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_event_review", columnNames = {"user_id", "event_id"})
        },
        indexes = {
                @Index(name = "idx_user", columnList = "user_id"),
                @Index(name = "idx_event", columnList = "event_id"),
                @Index(name = "idx_rating", columnList = "rating"),
                @Index(name = "idx_sentiment", columnList = "sentiment_label")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "sentiment_score", precision = 4, scale = 2)
    private BigDecimal sentimentScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_label", length = 20)
    private SentimentLabel sentimentLabel;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "is_reported", nullable = false)
    @Builder.Default
    private Boolean isReported = false;

    @Column(name = "moderation_notes", columnDefinition = "TEXT")
    private String moderationNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public void approve() {
        this.isApproved = true;
    }

    public void reject(String reason) {
        this.isApproved = false;
        this.moderationNotes = reason;
    }

    public void report() {
        this.isReported = true;
    }
}