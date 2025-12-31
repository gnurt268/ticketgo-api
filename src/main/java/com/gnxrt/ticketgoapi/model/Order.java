package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.PaymentMethod;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_event", columnList = "event_id"),
        @Index(name = "idx_order_code", columnList = "order_code"),
        @Index(name = "idx_payment_status", columnList = "payment_status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.VNPAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_transaction_id", length = 255)
    private String paymentTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "buyer_name", nullable = false, length = 255)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false, length = 255)
    private String buyerEmail;

    @Column(name = "buyer_phone", nullable = false, length = 20)
    private String buyerPhone;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    // Helper methods
    public boolean isPaid() {
        return this.paymentStatus == PaymentStatus.COMPLETED;
    }

    public void markAsPaid(String transactionId) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentTransactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void cancel() {
        this.paymentStatus = PaymentStatus.CANCELLED;
    }
}