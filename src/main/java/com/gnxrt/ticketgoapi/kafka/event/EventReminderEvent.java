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
public class EventReminderEvent extends BaseEmailEvent {

    private Long ticketId;
    private String ticketCode;
    private String qrCode;
    private String holderName;
    private String holderEmail;
    private String zoneName;
    private String seatCode;
    private String rowNumber;

    private Long relatedEventId;
    private String eventTitle;
    private String eventVenue;
    private String eventAddress;
    private LocalDateTime eventStartDate;
    private String eventPosterUrl;

    private int hoursBeforeEvent;

    public static EventReminderEvent create() {
        EventReminderEvent event = new EventReminderEvent();
        event.initializeEvent("EVENT_REMINDER");
        return event;
    }
}