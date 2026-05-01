package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.response.payment.VNPayCallbackDTO;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.model.Payment;
import com.gnxrt.ticketgoapi.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reconcile Payment PENDING bị miss callback VNPay (network fail, user đóng tab…).
 * Chạy mỗi 5 phút: với mỗi Payment PENDING tồn tại quá RECONCILE_AFTER_MINUTES,gọi VNPay querydr API để lấy trạng thái thực và đồng bộ Payment + Order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final OrderService orderService;

    private static final int RECONCILE_AFTER_MINUTES = 10;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @SchedulerLock(name = "payment-reconcile", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void reconcilePendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(RECONCILE_AFTER_MINUTES);

        List<Payment> stalePayments = paymentRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold);

        if (stalePayments.isEmpty()) {
            return;
        }

        log.info("Reconciling {} stale PENDING payments", stalePayments.size());

        for (Payment payment : stalePayments) {
            try {
                Optional<VNPayCallbackDTO> result = vnPayService.queryTransaction(payment);
                if (result.isEmpty()) {
                    log.info("Payment still pending at VNPay, retry next cycle: vnpTxnRef={}", payment.getVnpTxnRef());
                    continue;
                }
                orderService.applyQuerydrResult(payment.getId(), result.get());
                log.info("Payment reconciled: vnpTxnRef={}", payment.getVnpTxnRef());
            } catch (Exception e) {
                log.error("Failed to reconcile payment vnpTxnRef={}", payment.getVnpTxnRef(), e);
            }
        }
    }
}
