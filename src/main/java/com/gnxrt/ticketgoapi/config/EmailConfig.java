package com.gnxrt.ticketgoapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.email")
public class EmailConfig {

    private String fromAddress = "noreply@ticketgo.vn";

    private String fromName = "TicketGo";

    private String frontendUrl;

    private String logoUrl = "https://ticketgo.vn/logo.png";

    private String supportEmail = "support@ticketgo.vn";

    private String hotline = "1900 1234";
}