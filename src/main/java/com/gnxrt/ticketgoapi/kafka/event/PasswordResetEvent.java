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
public class PasswordResetEvent extends BaseEmailEvent {

    private Long userId;
    private String email;
    private String fullName;
    private String resetToken;

    public static PasswordResetEvent create() {
        PasswordResetEvent event = new PasswordResetEvent();
        event.initializeEvent("PASSWORD_RESET");
        return event;
    }
}