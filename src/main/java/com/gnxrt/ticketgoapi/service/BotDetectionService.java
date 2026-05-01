package com.gnxrt.ticketgoapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BotDetectionService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    public BotDetectionService(@Qualifier("aiServiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isBot(String fingerprint, String userAgent, String ipAddress,
                         boolean isAuthenticated, long timeSinceOpenSec, double requestsPerMinute) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("fingerprint", fingerprint);
            body.put("user_agent", userAgent);
            body.put("ip_address", ipAddress);
            body.put("is_authenticated", isAuthenticated);
            body.put("time_since_event_open_sec", timeSinceOpenSec);
            body.put("requests_per_minute", requestsPerMinute);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    aiServiceUrl + "/api/bot/detect",
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    MAP_TYPE);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean isBot = (Boolean) response.getBody().get("is_bot");
                String riskLevel = (String) response.getBody().get("risk_level");
                log.info("Bot detection: isBot={}, risk={}", isBot, riskLevel);
                return Boolean.TRUE.equals(isBot);
            }
        } catch (Exception e) {
            log.warn("Bot detection service unavailable, skipping: {}", e.getMessage());
        }
        return false;
    }
}
