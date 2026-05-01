package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.response.payment.RefundResponseDTO;
import com.gnxrt.ticketgoapi.enums.PaymentAuditAction;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.*;
import com.gnxrt.ticketgoapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PaymentAuditLogRepository auditLogRepository;
    private final VNPayService vnPayService;
    private final EmailService emailService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundResponseDTO refundOrder(Long orderId, String reason) {
        log.info("Initiating refund for order id: {}, reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException("Order đã được hoàn tiền trước đó");
        }
        if (order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Chỉ hoàn tiền được order đã thanh toán thành công (hiện tại: "
                    + order.getPaymentStatus() + ")");
        }

        Payment payment = paymentRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.COMPLETED)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy payment COMPLETED cho order " + orderId));

        User operator = resolveOperator();
        String operatorName = operator != null ? operator.getEmail() : "system";
        PaymentStatus oldStatus = payment.getStatus();

        String refundTxnId;
        try {
            refundTxnId = vnPayService.refundTransaction(payment, payment.getAmount(), reason, operatorName);
        } catch (VNPayService.VNPayRefundException ex) {
            log.error("VNPay refund failed for order {}: {}", orderId, ex.getMessage());
            writeAudit(payment, order, operator, PaymentAuditAction.REFUND_FAILED,
                    oldStatus, oldStatus, reason, ex.getMessage());
            throw new BadRequestException("Hoàn tiền VNPay thất bại: " + ex.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(now);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundTransactionId(refundTxnId);
        payment.setRefundReason(reason);
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() != TicketStatus.REFUNDED) {
                ticket.setStatus(TicketStatus.REFUNDED);
            }
        }
        ticketRepository.saveAll(tickets);

        writeAudit(payment, order, operator, PaymentAuditAction.REFUND,
                oldStatus, PaymentStatus.REFUNDED, reason,
                "refundTxnId=" + refundTxnId + ", tickets=" + tickets.size());

        emailService.sendOrderRefundedEmail(order, payment, reason);

        log.info("Refund completed for order {} (refundTxnId={})", orderId, refundTxnId);

        return RefundResponseDTO.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .paymentId(payment.getId())
                .refundAmount(payment.getRefundAmount())
                .refundTransactionId(refundTxnId)
                .refundedAt(now)
                .reason(reason)
                .status(PaymentStatus.REFUNDED.name())
                .build();
    }

    private void writeAudit(Payment payment, Order order, User operator,
                             PaymentAuditAction action, PaymentStatus oldStatus,
                             PaymentStatus newStatus, String reason, String details) {
        PaymentAuditLog audit = PaymentAuditLog.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .amount(payment.getAmount())
                .operatorId(operator != null ? operator.getId() : null)
                .reason(reason)
                .details(details)
                .build();
        auditLogRepository.save(audit);
    }

    private User resolveOperator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
