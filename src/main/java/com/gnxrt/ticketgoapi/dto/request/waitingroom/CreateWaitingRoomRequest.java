package com.gnxrt.ticketgoapi.dto.request.waitingroom;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWaitingRoomRequest {

    @NotNull(message = "Event ID không được để trống")
    private Long eventId;

    @Future(message = "Thời gian mở phòng chờ phải trong tương lai")
    private LocalDateTime preQueueStart;

    @NotNull(message = "Thời gian mở bán không được để trống")
    @Future(message = "Thời gian mở bán phải trong tương lai")
    private LocalDateTime saleStart;

    private LocalDateTime saleEnd;

    @Min(value = 10, message = "Throughput tối thiểu là 10 users/phút")
    @Max(value = 5000, message = "Throughput tối đa là 5000 users/phút")
    @Builder.Default
    private Integer throughputPerMinute = 500;

    @Min(value = 50, message = "Max concurrent users tối thiểu là 50")
    @Max(value = 10000, message = "Max concurrent users tối đa là 10000")
    @Builder.Default
    private Integer maxConcurrentUsers = 1000;

    @Min(value = 5, message = "Session timeout tối thiểu là 5 phút")
    @Max(value = 30, message = "Session timeout tối đa là 30 phút")
    @Builder.Default
    private Integer sessionTimeoutMinutes = 10;
}