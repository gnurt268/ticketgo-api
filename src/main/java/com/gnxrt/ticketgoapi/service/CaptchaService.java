package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${captcha.enabled:false}")
    private boolean enabled;

    @Value("${captcha.secret-key:}")
    private String secretKey;

    @Value("${captcha.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    public boolean isRequired() {
        return enabled && secretKey != null && !secretKey.isBlank();
    }

    public void verifyOrThrow(String captchaToken, String remoteIp) {
        if (!enabled) {
            return;
        }

        if (secretKey == null || secretKey.isBlank()) {
            log.error("CAPTCHA is enabled but captcha.secret-key is not configured");
            throw new ServiceUnavailableException("Dịch vụ CAPTCHA chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            throw new BadRequestException("Vui lòng hoàn tất xác minh CAPTCHA");
        }

        Map<String, Object> body;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secretKey);
            form.add("response", captchaToken);
            if (remoteIp != null && !remoteIp.isBlank()) {
                form.add("remoteip", remoteIp);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    verifyUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    MAP_TYPE
            );
            body = response.getBody();
        } catch (RestClientException ex) {
            log.warn("CAPTCHA verification unavailable: {}", ex.getMessage());
            throw new ServiceUnavailableException("Không thể kết nối dịch vụ CAPTCHA. Vui lòng thử lại sau.");
        }

        boolean success = body != null && Boolean.TRUE.equals(body.get("success"));
        if (!success) {
            List<String> errorCodes = getErrorCodes(body);
            log.warn("CAPTCHA verification failed: {}", errorCodes);
            throw new BadRequestException("Xác minh CAPTCHA không hợp lệ. Vui lòng thử lại.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getErrorCodes(Map<String, Object> body) {
        if (body == null || !(body.get("error-codes") instanceof List<?> errors)) {
            return List.of("unknown");
        }
        return errors.stream().map(Object::toString).toList();
    }
}
