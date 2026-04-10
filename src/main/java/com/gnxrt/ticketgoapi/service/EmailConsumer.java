package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.EmailConfig;
import com.gnxrt.ticketgoapi.config.KafkaConfig;
import com.gnxrt.ticketgoapi.dto.event.EmailEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;

    @KafkaListener(topics = KafkaConfig.EMAIL_TOPIC, groupId = "ticketgo-email-group")
    public void consumeEmailEvent(EmailEvent event) {
        log.info("Received email event [{}] for: {}", event.getType(), event.getTo());

        try {
            Context context = createBaseContext();

            if (event.getTemplateVariables() != null) {
                event.getTemplateVariables().forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process(event.getTemplateName(), context);

            if (event.getInlineImages() != null && !event.getInlineImages().isEmpty()) {
                sendHtmlEmailWithInlineImages(event.getTo(), event.getSubject(), htmlContent, event.getInlineImages());
            } else {
                sendHtmlEmail(event.getTo(), event.getSubject(), htmlContent);
            }

            log.info("Email [{}] sent successfully to: {}", event.getType(), event.getTo());
        } catch (Exception e) {
            log.error("Failed to process email event [{}] for: {}", event.getType(), event.getTo(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(emailConfig.getFromAddress());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private void sendHtmlEmailWithInlineImages(String to, String subject, String htmlContent,
                                               Map<String, byte[]> inlineImages) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(emailConfig.getFromAddress());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        for (Map.Entry<String, byte[]> entry : inlineImages.entrySet()) {
            helper.addInline(entry.getKey(),
                    new org.springframework.core.io.ByteArrayResource(entry.getValue()),
                    "image/png");
        }

        mailSender.send(message);
    }

    private Context createBaseContext() {
        Context context = new Context();
        context.setVariable("logoUrl", emailConfig.getLogoUrl());
        context.setVariable("frontendUrl", emailConfig.getFrontendUrl());
        context.setVariable("supportEmail", emailConfig.getSupportEmail());
        context.setVariable("hotline", emailConfig.getHotline());
        context.setVariable("currentYear", java.time.Year.now().getValue());
        return context;
    }
}
