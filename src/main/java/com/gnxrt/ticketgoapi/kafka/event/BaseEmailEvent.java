package com.gnxrt.ticketgoapi.kafka.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderConfirmationEvent.class, name = "ORDER_CONFIRMATION"),
        @JsonSubTypes.Type(value = PaymentSuccessEvent.class, name = "PAYMENT_SUCCESS"),
        @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PAYMENT_FAILED"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "ORDER_CANCELLED"),
        @JsonSubTypes.Type(value = TicketEmailEvent.class, name = "TICKET_EMAIL"),
        @JsonSubTypes.Type(value = TicketTransferEvent.class, name = "TICKET_TRANSFER"),
        @JsonSubTypes.Type(value = EventReminderEvent.class, name = "EVENT_REMINDER"),
        @JsonSubTypes.Type(value = EventCancelledEvent.class, name = "EVENT_CANCELLED"),
        @JsonSubTypes.Type(value = EventApprovedEvent.class, name = "EVENT_APPROVED"),
        @JsonSubTypes.Type(value = EventRejectedEvent.class, name = "EVENT_REJECTED"),
        @JsonSubTypes.Type(value = WelcomeEmailEvent.class, name = "WELCOME"),
        @JsonSubTypes.Type(value = VerificationEmailEvent.class, name = "VERIFICATION"),
        @JsonSubTypes.Type(value = PasswordResetEvent.class, name = "PASSWORD_RESET")
})
public abstract class BaseEmailEvent implements Serializable {

    private String eventId;
    private String eventType;
    private LocalDateTime createdAt;
    private int retryCount;

    public void initializeEvent(String type) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = type;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}