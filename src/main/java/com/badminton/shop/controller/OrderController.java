package com.badminton.shop.controller;

import com.badminton.shop.dto.request.OrderRequest;
import com.badminton.shop.dto.response.ApiResponse;
import com.badminton.shop.dto.response.OrderResponse;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.entity.User;
import com.badminton.shop.enums.OrderStatus;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.UserRepository;
import com.badminton.shop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "API quản lý đơn hàng")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {
        Long userId = getUserId(userDetails);
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn hàng thành công", response));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Lấy danh sách đơn hàng của tôi")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByUser(userId, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết đơn hàng theo ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @GetMapping("/code/{orderCode}")
    @Operation(summary = "Lấy chi tiết đơn hàng theo mã đơn hàng")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(@PathVariable String orderCode) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByCode(orderCode)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn hàng")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả đơn hàng (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(page, size, status)));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái đơn hàng (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công",
                orderService.updateOrderStatus(id, status)));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", userDetails.getUsername()));
        return user.getId();
    }
}
