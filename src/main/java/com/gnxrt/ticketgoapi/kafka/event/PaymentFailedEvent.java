package com.gnxrt.ticketgoapi.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends BaseEmailEvent {

    private Long orderId;
    private String orderCode;
    private String buyerEmail;
    private String buyerName;
    private BigDecimal totalAmount;
    private String reason;

    // Event info
    private Long relatedEventId;
    private String eventTitle;
    private LocalDateTime eventStartDate;

    public static PaymentFailedEvent create() {
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.initializeEvent("PAYMENT_FAILED");
        return event;
    }
}