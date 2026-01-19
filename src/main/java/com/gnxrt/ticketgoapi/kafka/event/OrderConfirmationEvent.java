package com.gnxrt.ticketgoapi.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderConfirmationEvent extends BaseEmailEvent {

    private Long orderId;
    private String orderCode;
    private String buyerEmail;
    private String buyerName;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime orderDate;

    private Long relatedEventId;
    private String eventTitle;
    private String eventVenue;
    private String eventAddress;
    private LocalDateTime eventStartDate;
    private String eventPosterUrl;

    private List<TicketInfo> tickets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private Long ticketId;
        private String ticketCode;
        private String qrCode;
        private String holderName;
        private String holderEmail;
        private String zoneName;
        private String seatCode;
        private String rowNumber;
    }

    public static OrderConfirmationEvent create() {
        OrderConfirmationEvent event = new OrderConfirmationEvent();
        event.initializeEvent("ORDER_CONFIRMATION");
        return event;
    }
}