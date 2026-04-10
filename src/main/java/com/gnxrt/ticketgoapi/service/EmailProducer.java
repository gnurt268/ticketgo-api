package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.KafkaConfig;
import com.gnxrt.ticketgoapi.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProducer {

    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    public void sendEmailEvent(EmailEvent event) {
        log.info("Publishing email event [{}] to: {}", event.getType(), event.getTo());
        kafkaTemplate.send(KafkaConfig.EMAIL_TOPIC, event.getTo(), event);
    }
}
