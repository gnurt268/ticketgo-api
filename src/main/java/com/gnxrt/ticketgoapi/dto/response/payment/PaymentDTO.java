package com.gnxrt.ticketgoapi.dto.response.payment;

import com.gnxrt.ticketgoapi.enums.PaymentMethod;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private String orderCode;

    private Long orderId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private PaymentMethod paymentMethod;

    private String paymentUrl;

    private String transactionId;

    private String bankCode;

    private String bankTransactionNo;

    private String cardType;

    private LocalDateTime paymentTime;

    private String message;

    private Integer responseCode;

    private LocalDateTime expiredAt;

    private Integer remainingSeconds;
}