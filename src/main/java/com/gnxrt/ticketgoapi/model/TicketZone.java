package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.ZoneType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket_zones",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_event_zone", columnNames = {"event_id", "zone_code"})
        },
        indexes = {
                @Index(name = "idx_event", columnList = "event_id"),
                @Index(name = "idx_zone_code", columnList = "zone_code"),
                @Index(name = "idx_price", columnList = "price")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "zone_code", nullable = false, length = 50)
    private String zoneCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "color_code", length = 7)
    @Builder.Default
    private String colorCode = "#3B82F6";

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @Column(name = "reserved_capacity", nullable = false)
    @Builder.Default
    private Integer reservedCapacity = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", length = 20)
    @Builder.Default
    private ZoneType zoneType = ZoneType.STANDARD;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "sale_start_date")
    private LocalDateTime saleStartDate;

    @Column(name = "sale_end_date")
    private LocalDateTime saleEndDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ticketZone", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "ticketZone", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    public boolean hasAvailableCapacity(Integer quantity) {
        return this.availableCapacity >= quantity;
    }

    public void reserveCapacity(Integer quantity) {
        this.availableCapacity -= quantity;
        this.reservedCapacity += quantity;
    }

    public void releaseReservation(Integer quantity) {
        this.availableCapacity += quantity;
        this.reservedCapacity -= quantity;
    }

    public void confirmSale(Integer quantity) {
        this.reservedCapacity -= quantity;
    }

    public boolean isSaleActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = saleStartDate == null || now.isAfter(saleStartDate);
        boolean beforeEnd = saleEndDate == null || now.isBefore(saleEndDate);
        return afterStart && beforeEnd && isActive;
    }
}