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
public class EventRejectedEvent extends BaseEmailEvent {

    // Organizer info
    private Long organizerId;
    private String organizerName;
    private String organizerEmail;

    // Event info
    private Long relatedEventId;
    private String eventTitle;
    private LocalDateTime eventStartDate;

    // Rejection reason
    private String reason;

    public static EventRejectedEvent create() {
        EventRejectedEvent event = new EventRejectedEvent();
        event.initializeEvent("EVENT_REJECTED");
        return event;
    }
}