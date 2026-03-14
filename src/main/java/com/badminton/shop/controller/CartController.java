package com.badminton.shop.controller;

import com.badminton.shop.dto.request.CartItemRequest;
import com.badminton.shop.dto.response.ApiResponse;
import com.badminton.shop.dto.response.CartResponse;
import com.badminton.shop.entity.User;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.UserRepository;
import com.badminton.shop.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "API quản lý giỏ hàng")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lấy giỏ hàng của tôi")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(cartService.getCartByUserId(userId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ hàng thành công",
                cartService.addItemToCart(userId, request)));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Cập nhật số lượng sản phẩm trong giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giỏ hàng thành công",
                cartService.updateCartItemQuantity(userId, productId, quantity)));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng")
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công",
                cartService.removeItemFromCart(userId, productId)));
    }

    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa giỏ hàng thành công"));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", userDetails.getUsername()));
        return user.getId();
    }
}
