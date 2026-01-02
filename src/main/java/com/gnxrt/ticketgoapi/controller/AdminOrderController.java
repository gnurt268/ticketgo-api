package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.response.order.OrderDTO;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * GET /api/admin/orders
     */
    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PaymentStatus paymentStatus
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderDTO> orders = orderService.getAllOrdersForAdmin(keyword, paymentStatus, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * GET /api/admin/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderByIdForAdmin(id);
        return ResponseEntity.ok(order);
    }

    /**
     * GET /api/admin/orders/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }
}