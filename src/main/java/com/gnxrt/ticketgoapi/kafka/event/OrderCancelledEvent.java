package com.gnxrt.ticketgoapi.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends BaseEmailEvent {

    private Long orderId;
    private String orderCode;
    private String buyerEmail;
    private String buyerName;
    private BigDecimal totalAmount;
    private String reason;

    // Event info
    private Long relatedEventId;
    private String eventTitle;

    public static OrderCancelledEvent create() {
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.initializeEvent("ORDER_CANCELLED");
        return event;
    }
}