package com.gnxrt.ticketgoapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Blacklist JWT token qua Redis dựa trên JTI.
 * Key format: {@code bl:<jti>} với TTL bằng thời gian còn lại của token.
 * Tự cleanup khi TTL hết hạn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "bl:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void blacklist(String jti, Duration ttl) {
        if (jti == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
        log.debug("Blacklisted jti={} for {}", jti, ttl);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}
