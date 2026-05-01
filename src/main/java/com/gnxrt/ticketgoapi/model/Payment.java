package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.PaymentMethod;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "order_id"),
        @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_vnp_txn_ref", columnList = "vnp_txn_ref", unique = true),
        @Index(name = "idx_payment_order_status_created", columnList = "order_id, status, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.VNPAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "vnp_txn_ref", length = 100, unique = true)
    private String vnpTxnRef;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "bank_transaction_no", length = 255)
    private String bankTransactionNo;

    @Column(name = "card_type", length = 50)
    private String cardType;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_transaction_id", length = 255)
    private String refundTransactionId;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
