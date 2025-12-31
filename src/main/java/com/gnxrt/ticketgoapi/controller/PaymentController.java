package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.response.order.OrderDTO;
import com.gnxrt.ticketgoapi.dto.response.payment.PaymentDTO;
import com.gnxrt.ticketgoapi.dto.response.payment.VNPayCallbackDTO;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.service.OrderService;
import com.gnxrt.ticketgoapi.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final VNPayService vnPayService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * GET /api/payment/vnpay/return
     */
    @GetMapping("/vnpay/return")
    public void vnPayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("VNPay return callback received");

        try {
            OrderDTO order = orderService.processPaymentCallback(request);

            String redirectUrl = frontendUrl + "/payment/success?orderCode=" + order.getOrderCode();
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Payment processing error", e);

            String orderCode = request.getParameter("vnp_TxnRef");

            String redirectUrl = frontendUrl + "/payment/failed?orderCode=" + orderCode + "&message=" + e.getMessage();
            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * GET /api/payment/vnpay/ipn
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnPayIPN(HttpServletRequest request) {
        log.info("VNPay IPN callback received");

        Map<String, String> result = new HashMap<>();

        try {
            if (!vnPayService.validateSignature(request)) {
                log.warn("Invalid IPN signature");
                result.put("RspCode", "97");
                result.put("Message", "Invalid signature");
                return ResponseEntity.ok(result);
            }

            VNPayCallbackDTO callback = vnPayService.processCallback(request);

            try {
                orderService.processPaymentCallback(request);
            } catch (Exception e) {
                log.info("Order already processed or error: {}", e.getMessage());
            }

            if (callback.isSuccess()) {
                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            } else {
                result.put("RspCode", "00");
                result.put("Message", "Confirm Success");
            }

        } catch (Exception e) {
            log.error("IPN processing error", e);
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/payment/status/{orderCode}
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<PaymentDTO> getPaymentStatus(@PathVariable String orderCode) {
        log.info("Getting payment status for order: {}", orderCode);

        OrderDTO order = orderService.getOrderByCode(orderCode);

        PaymentDTO payment = PaymentDTO.builder()
                .orderCode(order.getOrderCode())
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .transactionId(order.getPaymentTransactionId())
                .paymentTime(order.getPaidAt())
                .expiredAt(order.getPaymentExpiredAt())
                .remainingSeconds(order.getRemainingSeconds())
                .message(getStatusMessage(order.getPaymentStatus()))
                .build();

        return ResponseEntity.ok(payment);
    }

    /**
     * POST /api/payment/retry/{orderCode}
     */
    @PostMapping("/retry/{orderCode}")
    public ResponseEntity<PaymentDTO> retryPayment(
            @PathVariable String orderCode,
            HttpServletRequest request
    ) {
        log.info("Retrying payment for order: {}", orderCode);

        OrderDTO order = orderService.getOrderByCode(orderCode);

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Can only retry pending orders");
        }

        if (order.getRemainingSeconds() <= 0) {
            throw new RuntimeException("Order has expired. Please create a new order.");
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                order.getOrderCode(),
                order.getTotalAmount(),
                "Thanh toan ve su kien: " + order.getEventTitle(),
                vnPayService.getIpAddress(request)
        );

        PaymentDTO payment = PaymentDTO.builder()
                .orderCode(order.getOrderCode())
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentUrl(paymentUrl)
                .expiredAt(order.getPaymentExpiredAt())
                .remainingSeconds(order.getRemainingSeconds())
                .message("Payment URL generated successfully")
                .build();

        return ResponseEntity.ok(payment);
    }

    private String getStatusMessage(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Đang chờ thanh toán";
            case PROCESSING -> "Đang xử lý";
            case COMPLETED -> "Thanh toán thành công";
            case FAILED -> "Thanh toán thất bại";
            case CANCELLED -> "Đã hủy";
            case REFUNDED -> "Đã hoàn tiền";
            case EXPIRED -> "Hết hạn thanh toán";
        };
    }
}