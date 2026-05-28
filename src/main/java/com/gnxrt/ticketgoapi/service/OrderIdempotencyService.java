package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.order.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Idempotency cho createOrder — chống double-submit / double-order.
 * Ưu tiên header `Idempotency-Key` do client gửi; nếu không có thì derive key
 * từ (userId + eventId + zone/seats) để vẫn chặn double-click cùng một giỏ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdempotencyService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "idem:order:";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    // Bằng cửa sổ thanh toán (PAYMENT_TIMEOUT_MINUTES = 15) — sau khi đơn hết hạn, user được phép tạo đơn mới cho cùng giỏ.
    private static final Duration TTL = Duration.ofMinutes(15);

    public String resolveKey(String idempotencyHeader, Long userId, CreateOrderRequest request) {
        if (idempotencyHeader != null && !idempotencyHeader.isBlank()) {
            return KEY_PREFIX + idempotencyHeader.trim();
        }

        String cart;
        if (request.getSeatIds() != null && !request.getSeatIds().isEmpty()) {
            cart = request.getSeatIds().stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining("-"));
        } else {
            int qty = request.getQuantity() != null ? request.getQuantity() : 1;
            cart = "z" + request.getTicketZoneId() + "q" + qty;
        }
        return KEY_PREFIX + "auto:" + userId + ":" + request.getEventId() + ":" + cart;
    }

    /**
     * Đánh dấu bắt đầu xử lý. Trả false nếu key đã tồn tại (đang xử lý hoặc đã hoàn tất).
     */
    public boolean tryBegin(String key) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, IN_PROGRESS, TTL);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * Trả về orderCode nếu request trước đó với cùng key đã tạo đơn thành công.
     * Trả null nếu chưa có hoặc vẫn đang xử lý.
     */
    public String getCompletedOrderCode(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || IN_PROGRESS.equals(value)) {
            return null;
        }
        return value;
    }

    public void markCompleted(String key, String orderCode) {
        stringRedisTemplate.opsForValue().set(key, orderCode, TTL);
    }

    /** Gọi khi tạo đơn thất bại để cho phép retry ngay. */
    public void release(String key) {
        stringRedisTemplate.delete(key);
    }
}
