package com.gnxrt.ticketgoapi.model;

import com.gnxrt.ticketgoapi.enums.PaymentAuditAction;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_audit_logs", indexes = {
        @Index(name = "idx_pal_payment", columnList = "payment_id"),
        @Index(name = "idx_pal_order", columnList = "order_id"),
        @Index(name = "idx_pal_user", columnList = "user_id"),
        @Index(name = "idx_pal_action_created", columnList = "action, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private PaymentAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private PaymentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private PaymentStatus newStatus;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
