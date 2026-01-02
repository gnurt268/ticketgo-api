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
public class VerificationEmailEvent extends BaseEmailEvent {

    private Long userId;
    private String email;
    private String fullName;
    private String verificationToken;

    public static VerificationEmailEvent create() {
        VerificationEmailEvent event = new VerificationEmailEvent();
        event.initializeEvent("VERIFICATION");
        return event;
    }
}