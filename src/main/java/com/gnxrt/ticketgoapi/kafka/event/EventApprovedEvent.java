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
public class EventApprovedEvent extends BaseEmailEvent {

    private Long organizerId;
    private String organizerName;
    private String organizerEmail;

    private Long relatedEventId;
    private String eventTitle;
    private String eventSlug;
    private LocalDateTime eventStartDate;
    private String eventVenue;

    public static EventApprovedEvent create() {
        EventApprovedEvent event = new EventApprovedEvent();
        event.initializeEvent("EVENT_APPROVED");
        return event;
    }
}