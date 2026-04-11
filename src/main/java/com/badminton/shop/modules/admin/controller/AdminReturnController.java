package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.order.dto.request.ReceiveReturnRequest;
import com.badminton.shop.modules.order.dto.response.ReturnRequestResponse;
import com.badminton.shop.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReturnRequestResponse>>> getReturns(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReturnRequestResponse> response = orderService.adminGetReturns(keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Get returns successful", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getReturnStats() {
        Map<String, Long> stats = orderService.adminGetReturnStats();
        return ResponseEntity.ok(ApiResponse.success("Get return stats successful", stats));
    }

    @PostMapping("/{returnRequestId}/approve")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> approveReturnRequest(
            Principal principal,
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String note
    ) {
        ReturnRequestResponse response = orderService.approveReturnRequest(returnRequestId, principal.getName(), note);
        return ResponseEntity.ok(ApiResponse.success("Return request approved.", response));
    }

    @PostMapping("/{returnRequestId}/reject")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> rejectReturnRequest(
            Principal principal,
            @PathVariable Long returnRequestId,
            @RequestParam String note
    ) {
        ReturnRequestResponse response = orderService.rejectReturnRequest(returnRequestId, principal.getName(), note);
        return ResponseEntity.ok(ApiResponse.success("Return request rejected.", response));
    }

    @PostMapping("/{returnRequestId}/receive")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> receiveReturnedItems(
            Principal principal,
            @PathVariable Long returnRequestId,
            @Valid @RequestBody ReceiveReturnRequest request
    ) {
        ReturnRequestResponse response = orderService.receiveReturnedItems(returnRequestId, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Returned items processed successfully.", response));
    }

    @PostMapping("/{returnRequestId}/refund")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> markReturnRefunded(
            Principal principal,
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String note
    ) {
        ReturnRequestResponse response = orderService.markReturnRefunded(returnRequestId, principal.getName(), note);
        return ResponseEntity.ok(ApiResponse.success("Refund marked successfully.", response));
    }
}
