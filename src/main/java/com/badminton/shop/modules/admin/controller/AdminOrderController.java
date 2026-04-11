package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.order.dto.response.OrderResponse;
import com.badminton.shop.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<OrderResponse> response = orderService.adminGetOrders(keyword, status, paymentStatus, paymentMethod, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success("Get orders successful", response));
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderCode) {
        OrderResponse response = orderService.adminGetOrder(orderCode);
        return ResponseEntity.ok(ApiResponse.success("Get order successful", response));
    }

    @PatchMapping("/{orderCode}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderCode,
            @RequestParam String status,
            @RequestParam(required = false) String note,
            Principal principal
    ) {
        OrderResponse response = orderService.adminUpdateOrderStatus(orderCode, status, note, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Update order status successful", response));
    }

    @PostMapping("/{orderCode}/assign-shipping")
    public ResponseEntity<ApiResponse<OrderResponse>> assignShipping(
            @PathVariable String orderCode,
            @RequestParam String shippingCode,
            @RequestParam String shippingProvider,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expectedDeliveryAt,
            Principal principal
    ) {
        OrderResponse response = orderService.adminAssignShipping(orderCode, shippingCode, shippingProvider, expectedDeliveryAt, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Assign shipping successful", response));
    }

    // In the future, GET /{orderCode}/timeline (if not included in OrderResponse histories)
}
