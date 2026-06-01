package com.gnxrt.ticketgoapi.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Phân công nhân viên (vai trò STAFF) cho một sự kiện cụ thể.
 * Quyết định phạm vi check-in: một STAFF chỉ soát được vé của sự kiện được giao.
 */
@Entity
@Table(name = "event_staff", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_staff", columnNames = {"event_id", "staff_user_id"})
}, indexes = {
        @Index(name = "idx_event_staff_event", columnList = "event_id"),
        @Index(name = "idx_event_staff_user", columnList = "staff_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private User staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
