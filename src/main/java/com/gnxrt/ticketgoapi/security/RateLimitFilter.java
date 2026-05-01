package com.gnxrt.ticketgoapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnxrt.ticketgoapi.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final List<Rule> RULES = List.of(
            new Rule("/api/auth/login", 5, Duration.ofMinutes(1)),
            new Rule("/api/auth/register", 3, Duration.ofMinutes(1)),
            new Rule("/api/", 100, Duration.ofMinutes(1))
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/ws/")) {
            chain.doFilter(request, response);
            return;
        }

        Rule rule = matchRule(path);

        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        long windowSeconds = rule.window.getSeconds();
        long currentWindow = System.currentTimeMillis() / 1000 / windowSeconds;
        String key = "ratelimit:" + rule.prefix + ":" + clientIp + ":" + currentWindow;

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, rule.window);
        }

        if (count != null && count > rule.limit) {
            log.warn("Rate limit exceeded: ip={} path={} count={} limit={}", clientIp, path, count, rule.limit);
            writeTooManyRequests(response, request.getRequestURI(), rule);
            return;
        }

        chain.doFilter(request, response);
    }

    private Rule matchRule(String path) {
        for (Rule rule : RULES) {
            if (path.startsWith(rule.prefix)) {
                return rule;
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String path, Rule rule) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("Bạn đã gửi quá nhiều request, vui lòng thử lại sau " + rule.window.getSeconds() + " giây.")
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(rule.window.getSeconds()));
        objectMapper.writeValue(response.getWriter(), body);
    }

    private record Rule(String prefix, int limit, Duration window) {}
}
