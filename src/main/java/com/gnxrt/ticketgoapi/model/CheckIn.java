package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "check_ins", indexes = {
        @Index(name = "idx_ticket", columnList = "ticket_id"),
        @Index(name = "idx_event", columnList = "event_id"),
        @Index(name = "idx_check_in_at", columnList = "check_in_at"),
        @Index(name = "idx_method", columnList = "check_in_method")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_method", nullable = false, length = 20)
    private CheckInMethod checkInMethod;

    @Column(name = "check_in_at", nullable = false)
    @Builder.Default
    private LocalDateTime checkInAt = LocalDateTime.now();

    @Column(name = "face_image_url", length = 500)
    private String faceImageUrl;

    @Column(name = "face_match_confidence", precision = 4, scale = 2)
    private BigDecimal faceMatchConfidence;

    @Column(name = "face_match_ticket_id")
    private Long faceMatchTicketId;

    @Column(name = "check_in_location", length = 255)
    private String checkInLocation;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_user_id")
    private User staff;

    @Column(name = "staff_notes", columnDefinition = "TEXT")
    private String staffNotes;

    @Column(name = "is_successful", nullable = false)
    @Builder.Default
    private Boolean isSuccessful = true;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}