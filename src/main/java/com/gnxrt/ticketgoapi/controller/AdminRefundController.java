package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.order.RefundRequest;
import com.gnxrt.ticketgoapi.dto.response.payment.RefundResponseDTO;
import com.gnxrt.ticketgoapi.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponseDTO> refundOrder(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponseDTO response = refundService.refundOrder(orderId, request.getReason());
        return ResponseEntity.ok(response);
    }
}
