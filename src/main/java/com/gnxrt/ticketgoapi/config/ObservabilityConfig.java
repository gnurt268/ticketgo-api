package com.gnxrt.ticketgoapi.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public Counter ticketReservationCounter(MeterRegistry registry) {
        return Counter.builder("ticket_reservation_total")
                .description("Số lần thử reserve ghế (bao gồm cả thành công và thất bại)")
                .tag("result", "attempt")
                .register(registry);
    }

    @Bean
    public Timer orderCreationTimer(MeterRegistry registry) {
        return Timer.builder("order_creation_duration_seconds")
                .description("Thời gian xử lý tạo Order end-to-end")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public MultiGauge waitingRoomDepthGauge(MeterRegistry registry) {
        return MultiGauge.builder("waiting_room_depth")
                .description("Số user đang chờ trong waiting room theo event")
                .baseUnit("users")
                .register(registry);
    }
}
