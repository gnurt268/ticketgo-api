package com.gnxrt.ticketgoapi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String EMAIL_TOPIC = "email-notifications";

    @Bean
    public NewTopic emailNotificationsTopic() {
        return TopicBuilder.name(EMAIL_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
