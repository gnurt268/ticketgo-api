package com.gnxrt.ticketgoapi.dto.response.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponseDTO {
    private Long orderId;
    private String orderCode;
    private Long paymentId;
    private BigDecimal refundAmount;
    private String refundTransactionId;
    private LocalDateTime refundedAt;
    private String reason;
    private String status;
}
