package com.gnxrt.ticketgoapi.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundRequest {
    @NotBlank(message = "Lý do hoàn tiền không được để trống")
    @Size(max = 500, message = "Lý do không vượt quá 500 ký tự")
    private String reason;
}
