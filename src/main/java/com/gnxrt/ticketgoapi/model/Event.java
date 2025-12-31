package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Event Entity
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_organizer", columnList = "organizer_id"),
        @Index(name = "idx_category", columnList = "category_id"),
        @Index(name = "idx_slug", columnList = "slug"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_start_date", columnList = "start_date"),
        @Index(name = "idx_city", columnList = "city"),
        @Index(name = "idx_featured", columnList = "is_featured"),
        @Index(name = "idx_event_type", columnList = "event_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 255)
    private String venue;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20)
    @Builder.Default
    private EventType eventType = EventType.OUTDOOR;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "max_tickets_per_order", nullable = false)
    @Builder.Default
    private Integer maxTicketsPerOrder = 10;

    @Column(name = "enable_seat_selection", nullable = false)
    @Builder.Default
    private Boolean enableSeatSelection = false;

    @Column(name = "seat_map_image_url", length = 500)
    private String seatMapImageUrl;

    @Column(name = "enable_face_recognition", nullable = false)
    @Builder.Default
    private Boolean enableFaceRecognition = true;

    @Column(name = "face_recognition_threshold", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal faceRecognitionThreshold = new BigDecimal("0.70");

    @Column(name = "require_face_upload", nullable = false)
    @Builder.Default
    private Boolean requireFaceUpload = true;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "total_tickets_sold", nullable = false)
    @Builder.Default
    private Integer totalTicketsSold = 0;

    @Column(name = "total_revenue", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketZone> ticketZones = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckIn> checkIns = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserEventInteraction> interactions = new ArrayList<>();

    // Helper methods
    public boolean isPublished() {
        return this.status == EventStatus.PUBLISHED;
    }

    public boolean isUpcoming() {
        return this.startDate.isAfter(LocalDateTime.now());
    }

    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return this.startDate.isBefore(now) && this.endDate.isAfter(now);
    }

    public boolean isPast() {
        return this.endDate.isBefore(LocalDateTime.now());
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void addTicketSold(Integer quantity, BigDecimal amount) {
        this.totalTicketsSold += quantity;
        this.totalRevenue = this.totalRevenue.add(amount);
    }
}