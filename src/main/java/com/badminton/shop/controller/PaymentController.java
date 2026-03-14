package com.badminton.shop.controller;

import com.badminton.shop.dto.response.ApiResponse;
import com.badminton.shop.dto.response.PaymentResponse;
import com.badminton.shop.enums.PaymentMethod;
import com.badminton.shop.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "API quản lý thanh toán")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Lấy thông tin thanh toán theo đơn hàng")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByOrderId(orderId)));
    }

    @PostMapping("/order/{orderId}/process")
    @Operation(summary = "Xử lý thanh toán")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable Long orderId,
            @RequestParam PaymentMethod method) {
        return ResponseEntity.ok(ApiResponse.success("Xử lý thanh toán thành công",
                paymentService.processPayment(orderId, method)));
    }

    @PostMapping("/admin/order/{orderId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xác nhận thanh toán (Admin)")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable Long orderId,
            @RequestParam String transactionCode) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán thành công",
                paymentService.confirmPayment(orderId, transactionCode)));
    }
}
