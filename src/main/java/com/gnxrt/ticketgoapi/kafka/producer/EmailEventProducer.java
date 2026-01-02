package com.gnxrt.ticketgoapi.kafka.producer;

import com.gnxrt.ticketgoapi.config.KafkaConfig;
import com.gnxrt.ticketgoapi.kafka.event.*;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Order;
import com.gnxrt.ticketgoapi.model.Ticket;
import com.gnxrt.ticketgoapi.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventProducer {

    private final KafkaTemplate<String, BaseEmailEvent> kafkaTemplate;

    public void sendOrderConfirmationEvent(Order order, List<Ticket> tickets) {
        OrderConfirmationEvent event = OrderConfirmationEvent.create();

        event.setOrderId(order.getId());
        event.setOrderCode(order.getOrderCode());
        event.setBuyerEmail(order.getBuyerEmail());
        event.setBuyerName(order.getBuyerName());
        event.setTotalAmount(order.getTotalAmount());
        event.setCurrency(order.getCurrency());
        event.setOrderDate(order.getCreatedAt());

        Event evt = order.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventVenue(evt.getVenue());
        event.setEventAddress(evt.getAddress());
        event.setEventStartDate(evt.getStartDate());
        event.setEventPosterUrl(evt.getPosterUrl());

        event.setTickets(tickets.stream()
                .map(t -> new OrderConfirmationEvent.TicketInfo(
                        t.getId(),
                        t.getTicketCode(),
                        t.getQrCode(),
                        t.getHolderName(),
                        t.getHolderEmail(),
                        t.getTicketZone().getZoneName(),
                        t.getSeatNumber(),
                        t.getRowNumber()
                ))
                .collect(Collectors.toList()));

        sendEvent(order.getOrderCode(), event);
    }

    public void sendPaymentSuccessEvent(Order order, List<Ticket> tickets) {
        PaymentSuccessEvent event = PaymentSuccessEvent.create();

        event.setOrderId(order.getId());
        event.setOrderCode(order.getOrderCode());
        event.setTransactionId(order.getPaymentTransactionId());
        event.setTotalAmount(order.getTotalAmount());
        event.setPaidAt(order.getPaidAt());

        Event evt = order.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventVenue(evt.getVenue());
        event.setEventAddress(evt.getAddress());
        event.setEventStartDate(evt.getStartDate());
        event.setEventPosterUrl(evt.getPosterUrl());

        event.setTickets(tickets.stream()
                .map(t -> new PaymentSuccessEvent.TicketEmailInfo(
                        t.getId(),
                        t.getTicketCode(),
                        t.getQrCode(),
                        t.getHolderName(),
                        t.getHolderEmail(),
                        t.getHolderPhone(),
                        t.getTicketZone().getZoneName(),
                        t.getSeatNumber(),
                        t.getRowNumber()
                ))
                .collect(Collectors.toList()));

        sendEvent(order.getOrderCode(), event);
    }

    public void sendPaymentFailedEvent(Order order, String reason) {
        PaymentFailedEvent event = PaymentFailedEvent.create();

        event.setOrderId(order.getId());
        event.setOrderCode(order.getOrderCode());
        event.setBuyerEmail(order.getBuyerEmail());
        event.setBuyerName(order.getBuyerName());
        event.setTotalAmount(order.getTotalAmount());
        event.setReason(reason);

        Event evt = order.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventStartDate(evt.getStartDate());

        sendEvent(order.getOrderCode(), event);
    }

    public void sendOrderCancelledEvent(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.create();

        event.setOrderId(order.getId());
        event.setOrderCode(order.getOrderCode());
        event.setBuyerEmail(order.getBuyerEmail());
        event.setBuyerName(order.getBuyerName());
        event.setTotalAmount(order.getTotalAmount());
        event.setReason(reason);

        Event evt = order.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());

        sendEvent(order.getOrderCode(), event);
    }

    public void sendTicketEmailEvent(Ticket ticket) {
        TicketEmailEvent event = TicketEmailEvent.create();

        event.setTicketId(ticket.getId());
        event.setTicketCode(ticket.getTicketCode());
        event.setQrCode(ticket.getQrCode());
        event.setHolderName(ticket.getHolderName());
        event.setHolderEmail(ticket.getHolderEmail());
        event.setHolderPhone(ticket.getHolderPhone());
        event.setZoneName(ticket.getTicketZone().getZoneName());
        event.setSeatCode(ticket.getSeatNumber());
        event.setRowNumber(ticket.getRowNumber());

        Event evt = ticket.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventVenue(evt.getVenue());
        event.setEventAddress(evt.getAddress());
        event.setEventStartDate(evt.getStartDate());
        event.setEventPosterUrl(evt.getPosterUrl());

        sendEvent(ticket.getTicketCode(), event);
    }

    public void sendTicketTransferEvent(Ticket ticket, String fromEmail, String fromName) {
        TicketTransferEvent event = TicketTransferEvent.create();

        event.setTicketId(ticket.getId());
        event.setTicketCode(ticket.getTicketCode());
        event.setQrCode(ticket.getQrCode());
        event.setFromEmail(fromEmail);
        event.setFromName(fromName);
        event.setToEmail(ticket.getHolderEmail());
        event.setToName(ticket.getHolderName());
        event.setToPhone(ticket.getHolderPhone());
        event.setZoneName(ticket.getTicketZone().getZoneName());
        event.setSeatCode(ticket.getSeatNumber());
        event.setRowNumber(ticket.getRowNumber());

        Event evt = ticket.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventVenue(evt.getVenue());
        event.setEventAddress(evt.getAddress());
        event.setEventStartDate(evt.getStartDate());
        event.setEventPosterUrl(evt.getPosterUrl());

        sendEvent(ticket.getTicketCode(), event);
    }

    public void sendEventReminderEvent(Ticket ticket, int hoursBeforeEvent) {
        EventReminderEvent event = EventReminderEvent.create();

        event.setTicketId(ticket.getId());
        event.setTicketCode(ticket.getTicketCode());
        event.setQrCode(ticket.getQrCode());
        event.setHolderName(ticket.getHolderName());
        event.setHolderEmail(ticket.getHolderEmail());
        event.setZoneName(ticket.getTicketZone().getZoneName());
        event.setSeatCode(ticket.getSeatNumber());
        event.setRowNumber(ticket.getRowNumber());
        event.setHoursBeforeEvent(hoursBeforeEvent);

        Event evt = ticket.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventVenue(evt.getVenue());
        event.setEventAddress(evt.getAddress());
        event.setEventStartDate(evt.getStartDate());
        event.setEventPosterUrl(evt.getPosterUrl());

        sendEvent(ticket.getTicketCode(), event);
    }

    public void sendEventCancelledEvent(Ticket ticket, String reason) {
        EventCancelledEvent event = EventCancelledEvent.create();

        event.setTicketId(ticket.getId());
        event.setTicketCode(ticket.getTicketCode());
        event.setHolderName(ticket.getHolderName());
        event.setHolderEmail(ticket.getHolderEmail());
        event.setZoneName(ticket.getTicketZone().getZoneName());
        event.setSeatCode(ticket.getSeatNumber());
        event.setReason(reason);

        Event evt = ticket.getEvent();
        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventStartDate(evt.getStartDate());

        sendEvent(ticket.getTicketCode(), event);
    }

    public void sendEventApprovedEvent(Event evt) {
        EventApprovedEvent event = EventApprovedEvent.create();

        User organizer = evt.getOrganizer();
        event.setOrganizerId(organizer.getId());
        event.setOrganizerName(organizer.getFullName());
        event.setOrganizerEmail(organizer.getEmail());

        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventSlug(evt.getSlug());
        event.setEventStartDate(evt.getStartDate());
        event.setEventVenue(evt.getVenue());

        sendEvent("event-" + evt.getId(), event);
    }

    public void sendEventRejectedEvent(Event evt, String reason) {
        EventRejectedEvent event = EventRejectedEvent.create();

        User organizer = evt.getOrganizer();
        event.setOrganizerId(organizer.getId());
        event.setOrganizerName(organizer.getFullName());
        event.setOrganizerEmail(organizer.getEmail());

        event.setRelatedEventId(evt.getId());
        event.setEventTitle(evt.getTitle());
        event.setEventStartDate(evt.getStartDate());
        event.setReason(reason);

        sendEvent("event-" + evt.getId(), event);
    }

    public void sendWelcomeEmailEvent(User user) {
        WelcomeEmailEvent event = WelcomeEmailEvent.create();

        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setFullName(user.getFullName());

        sendEvent("user-" + user.getId(), event);
    }

    public void sendVerificationEmailEvent(User user, String verificationToken) {
        VerificationEmailEvent event = VerificationEmailEvent.create();

        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setFullName(user.getFullName());
        event.setVerificationToken(verificationToken);

        sendEvent("user-" + user.getId(), event);
    }

    public void sendPasswordResetEvent(User user, String resetToken) {
        PasswordResetEvent event = PasswordResetEvent.create();

        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setFullName(user.getFullName());
        event.setResetToken(resetToken);

        sendEvent("user-" + user.getId(), event);
    }

    private void sendEvent(String key, BaseEmailEvent event) {
        log.info("Sending email event: type={}, key={}, eventId={}",
                event.getEventType(), key, event.getEventId());

        CompletableFuture<SendResult<String, BaseEmailEvent>> future =
                kafkaTemplate.send(KafkaConfig.EMAIL_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Email event sent successfully: type={}, eventId={}, partition={}, offset={}",
                        event.getEventType(),
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send email event: type={}, eventId={}, error={}",
                        event.getEventType(),
                        event.getEventId(),
                        ex.getMessage());
            }
        });
    }
}