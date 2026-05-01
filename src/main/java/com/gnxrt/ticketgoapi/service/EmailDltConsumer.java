package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.KafkaConfig;
import com.gnxrt.ticketgoapi.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDltConsumer {

    @KafkaListener(
            topics = KafkaConfig.EMAIL_DLT_TOPIC,
            groupId = "ticketgo-email-dlt-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDeadLetter(
            @Payload EmailEvent event,
            @Header(name = KafkaHeaders.ORIGINAL_TOPIC, required = false) byte[] originalTopic,
            @Header(name = KafkaHeaders.EXCEPTION_FQCN, required = false) byte[] exceptionClass,
            @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) byte[] exceptionMessage
    ) {
        log.error("[DLT] Email delivery failed permanently: type={}, to={}, subject={}, originalTopic={}, cause={}: {}",
                event != null ? event.getType() : null,
                event != null ? event.getTo() : null,
                event != null ? event.getSubject() : null,
                asString(originalTopic),
                asString(exceptionClass),
                asString(exceptionMessage));
    }

    private String asString(byte[] bytes) {
        return bytes == null ? null : new String(bytes);
    }
}
