package com.badminton.shop.modules.payment.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class VnpayCallbackController {

    private final OrderService orderService;

    @GetMapping("/vnpay-callback")
    public ResponseEntity<ApiResponse<Map<String, String>>> vnpayReturn(@RequestParam Map<String, String> params) {
        Map<String, String> response = orderService.handleVnpayReturn(params);
        return ResponseEntity.ok(ApiResponse.success("VNPAY return processed.", response));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(orderService.handleVnpayIpn(params));
    }
}
