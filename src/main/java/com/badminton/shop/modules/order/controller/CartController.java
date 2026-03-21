package com.badminton.shop.modules.order.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.order.dto.request.AddCartItemRequest;
import com.badminton.shop.modules.order.dto.request.UpdateCartItemRequest;
import com.badminton.shop.modules.order.dto.response.CartResponse;
import com.badminton.shop.modules.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Principal principal) {
		CartResponse response = cartService.getMyCart(principal.getName());
		return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully.", response));
	}

	@PostMapping("/items")
	public ResponseEntity<ApiResponse<CartResponse>> addItem(
			Principal principal,
			@Valid @RequestBody AddCartItemRequest request) {
		CartResponse response = cartService.addItem(principal.getName(), request);
		return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully.", response));
	}

	@PutMapping("/items/{variantId}")
	public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
			Principal principal,
			@PathVariable Long variantId,
			@Valid @RequestBody UpdateCartItemRequest request) {
		CartResponse response = cartService.updateItemQuantity(principal.getName(), variantId, request);
		return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated successfully.", response));
	}

	@DeleteMapping("/items/{variantId}")
	public ResponseEntity<ApiResponse<CartResponse>> removeItem(Principal principal, @PathVariable Long variantId) {
		CartResponse response = cartService.removeItem(principal.getName(), variantId);
		return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully.", response));
	}

	@DeleteMapping
	public ResponseEntity<ApiResponse<CartResponse>> clearCart(Principal principal) {
		CartResponse response = cartService.clearCart(principal.getName());
		return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully.", response));
	}
}
