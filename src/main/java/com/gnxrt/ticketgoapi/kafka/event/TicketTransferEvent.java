package com.gnxrt.ticketgoapi.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketTransferEvent extends BaseEmailEvent {

    private Long ticketId;
    private String ticketCode;
    private String qrCode;

    private String fromEmail;
    private String fromName;

    private String toEmail;
    private String toName;
    private String toPhone;

    private String zoneName;
    private String seatCode;
    private String rowNumber;

    private Long relatedEventId;
    private String eventTitle;
    private String eventVenue;
    private String eventAddress;
    private LocalDateTime eventStartDate;
    private String eventPosterUrl;

    public static TicketTransferEvent create() {
        TicketTransferEvent event = new TicketTransferEvent();
        event.initializeEvent("TICKET_TRANSFER");
        return event;
    }
}