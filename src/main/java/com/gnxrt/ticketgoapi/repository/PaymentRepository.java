package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    long countByOrderId(Long orderId);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, java.time.LocalDateTime before);
}
