package com.gnxrt.ticketgoapi.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WelcomeEmailEvent extends BaseEmailEvent {

    private Long userId;
    private String email;
    private String fullName;

    public static WelcomeEmailEvent create() {
        WelcomeEmailEvent event = new WelcomeEmailEvent();
        event.initializeEvent("WELCOME");
        return event;
    }
}