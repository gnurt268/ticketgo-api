package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.SeatStatus;
import com.gnxrt.ticketgoapi.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_seat", columnNames = {"ticket_zone_id", "seat_code"})
        },
        indexes = {
                @Index(name = "idx_zone", columnList = "ticket_zone_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_row", columnList = "row_label"),
                @Index(name = "idx_reservation", columnList = "reserved_by, reserved_until")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_zone_id", nullable = false)
    private TicketZone ticketZone;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "seat_code", nullable = false, length = 20)
    private String seatCode;

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 20)
    @Builder.Default
    private SeatType seatType = SeatType.STANDARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by")
    private User reservedBy;

    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "seat")
    private Ticket ticket;

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public boolean isReservationExpired() {
        return this.status == SeatStatus.RESERVED
                && this.reservedUntil != null
                && this.reservedUntil.isBefore(LocalDateTime.now());
    }

    public void reserve(User user, int minutesToExpire) {
        this.status = SeatStatus.RESERVED;
        this.reservedBy = user;
        this.reservedUntil = LocalDateTime.now().plusMinutes(minutesToExpire);
    }

    public void releaseReservation() {
        this.status = SeatStatus.AVAILABLE;
        this.reservedBy = null;
        this.reservedUntil = null;
    }

    public void markAsSold() {
        this.status = SeatStatus.SOLD;
        this.reservedBy = null;
        this.reservedUntil = null;
    }
}