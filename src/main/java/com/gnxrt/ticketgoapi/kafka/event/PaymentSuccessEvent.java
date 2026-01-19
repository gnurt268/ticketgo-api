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
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentSuccessEvent extends BaseEmailEvent {

    private Long orderId;
    private String orderCode;
    private String transactionId;
    private BigDecimal totalAmount;
    private LocalDateTime paidAt;

    private Long relatedEventId;
    private String eventTitle;
    private String eventVenue;
    private String eventAddress;
    private LocalDateTime eventStartDate;
    private String eventPosterUrl;

    private List<TicketEmailInfo> tickets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketEmailInfo {
        private Long ticketId;
        private String ticketCode;
        private String qrCode;
        private String holderName;
        private String holderEmail;
        private String holderPhone;
        private String zoneName;
        private String seatCode;
        private String rowNumber;
    }

    public static PaymentSuccessEvent create() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.initializeEvent("PAYMENT_SUCCESS");
        return event;
    }
}