package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_order", columnList = "order_id"),
        @Index(name = "idx_event", columnList = "event_id"),
        @Index(name = "idx_zone", columnList = "ticket_zone_id"),
        @Index(name = "idx_seat", columnList = "seat_id"),
        @Index(name = "idx_ticket_code", columnList = "ticket_code"),
        @Index(name = "idx_qr_code", columnList = "qr_code"),
        @Index(name = "idx_holder_email", columnList = "holder_email"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_checked_in", columnList = "is_checked_in")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_zone_id", nullable = false)
    private TicketZone ticketZone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "holder_name", nullable = false, length = 255)
    private String holderName;

    @Column(name = "holder_email", nullable = false, length = 255)
    private String holderEmail;

    @Column(name = "holder_phone", nullable = false, length = 20)
    private String holderPhone;

    @Column(name = "holder_id_number", length = 50)
    private String holderIdNumber;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    @Column(name = "seat_row", length = 10)
    private String rowNumber;

    @Column(name = "qr_code", nullable = false, unique = true, length = 500)
    private String qrCode;

    @Column(name = "face_image_url", length = 500)
    private String faceImageUrl;

    @Column(name = "face_uploaded_at")
    private LocalDateTime faceUploadedAt;

    @Column(name = "face_embedding_id")
    private Long faceEmbeddingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVE;

    @Column(name = "is_checked_in", nullable = false)
    @Builder.Default
    private Boolean isCheckedIn = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "checked_in_by", length = 20)
    private CheckInMethod checkedInBy;

    @Column(name = "checked_in_confidence", precision = 4, scale = 2)
    private BigDecimal checkedInConfidence;

    @Column(name = "transferred_from_email", length = 255)
    private String transferredFromEmail;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private FaceEmbedding faceEmbedding;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    @Builder.Default
    private java.util.List<CheckIn> checkIns = new java.util.ArrayList<>();


    public boolean isActive() {
        return this.status == TicketStatus.ACTIVE;
    }

    public void checkIn(CheckInMethod method, BigDecimal confidence) {
        this.isCheckedIn = true;
        this.checkedInAt = LocalDateTime.now();
        this.checkedInBy = method;
        this.checkedInConfidence = confidence;
        this.status = TicketStatus.USED;
    }

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }

    public String getSeatInfo() {
        if (seat != null) {
            return seat.getSeatCode();
        }
        if (seatNumber != null) {
            return rowNumber != null ? rowNumber + seatNumber : seatNumber;
        }
        return "General Admission";
    }
}