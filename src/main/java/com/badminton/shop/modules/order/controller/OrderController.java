package com.badminton.shop.modules.order.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.order.dto.request.CancelOrderRequest;
import com.badminton.shop.modules.order.dto.request.CreateReturnRequest;
import com.badminton.shop.modules.order.dto.request.CreateOrderRequest;
import com.badminton.shop.modules.order.dto.request.ReceiveReturnRequest;
import com.badminton.shop.modules.order.dto.response.CheckoutContextResponse;
import com.badminton.shop.modules.order.dto.response.OrderPreviewResponse;
import com.badminton.shop.modules.order.dto.response.OrderResponse;
import com.badminton.shop.modules.order.dto.response.ReturnRequestResponse;
import com.badminton.shop.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/checkout-context")
    public ResponseEntity<ApiResponse<CheckoutContextResponse>> getCheckoutContext(Principal principal) {
        CheckoutContextResponse response = orderService.getCheckoutContext(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Checkout context fetched successfully.", response));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
            Principal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderPreviewResponse response = orderService.previewOrder(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Order preview generated successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> purchase(
            Principal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.purchase(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully.", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrderResponse> response = orderService.getMyOrders(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Order history fetched successfully.", response));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<ApiResponse<Map<String, String>>> vnpayReturn(@RequestParam Map<String, String> params) {
        Map<String, String> response = orderService.handleVnpayReturn(params);
        return ResponseEntity.ok(ApiResponse.success("VNPAY return processed.", response));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(orderService.handleVnpayIpn(params));
    }

    @PostMapping("/{orderCode}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            Principal principal,
            @PathVariable String orderCode,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderResponse response = orderService.cancelOrderByUser(principal.getName(), orderCode, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully.", response));
    }

    @PostMapping("/{orderCode}/returns")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> createReturnRequest(
            Principal principal,
            @PathVariable String orderCode,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        ReturnRequestResponse response = orderService.createReturnRequest(principal.getName(), orderCode, request);
        return ResponseEntity.ok(ApiResponse.success("Return request submitted successfully.", response));
    }

    @PostMapping("/admin/returns/{returnRequestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> approveReturnRequest(
            Principal principal,
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String note
    ) {
        ReturnRequestResponse response = orderService.approveReturnRequest(returnRequestId, principal.getName(), note);
        return ResponseEntity.ok(ApiResponse.success("Return request approved.", response));
    }

    @PostMapping("/admin/returns/{returnRequestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> rejectReturnRequest(
            Principal principal,
            @PathVariable Long returnRequestId,
            @RequestParam String note
    ) {
        ReturnRequestResponse response = orderService.rejectReturnRequest(returnRequestId, principal.getName(), note);
        return ResponseEntity.ok(ApiResponse.success("Return request rejected.", response));
    }

    @PostMapping("/admin/returns/{returnRequestId}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> receiveReturnedItems(
            Principal principal,
            @PathVariable Long returnRequestId,
            @Valid @RequestBody ReceiveReturnRequest request
    ) {
        ReturnRequestResponse response = orderService.receiveReturnedItems(returnRequestId, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Returned items processed successfully.", response));
    }

    @PostMapping("/admin/returns/{returnRequestId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> markReturnRefunded(
            Principal principal,
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String note
    ) {
        ReturnRequestResponse response = orderService.markReturnRefunded(returnRequestId, principal.getName(), note);
        return ResponseEntity.ok(ApiResponse.success("Refund marked successfully.", response));
    }
}
