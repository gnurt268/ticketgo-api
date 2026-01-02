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
public class EventCancelledEvent extends BaseEmailEvent {

    private Long ticketId;
    private String ticketCode;
    private String holderName;
    private String holderEmail;
    private String zoneName;
    private String seatCode;
    private BigDecimal refundAmount;

    // Event info
    private Long relatedEventId;
    private String eventTitle;
    private LocalDateTime eventStartDate;
    private String reason;

    public static EventCancelledEvent create() {
        EventCancelledEvent event = new EventCancelledEvent();
        event.initializeEvent("EVENT_CANCELLED");
        return event;
    }
}