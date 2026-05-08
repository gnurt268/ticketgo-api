package com.gnxrt.ticketgoapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.email")
public class EmailConfig {

    private String fromAddress = "ticketgo88f1@gmail.com";

    private String fromName = "TicketGo";

    private String frontendUrl;

    private String logoUrl = "https://ticketgo.vn/logo.png";

    private String supportEmail = "ticketgo88f1@gmail.com";

    private String hotline = "1900 XXXX";
}