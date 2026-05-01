package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

    List<PaymentAuditLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    List<PaymentAuditLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
