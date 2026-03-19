package com.badminton.shop.modules.order.controller;

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
	public ResponseEntity<CartResponse> getMyCart(Principal principal) {
		return ResponseEntity.ok(cartService.getMyCart(principal.getName()));
	}

	@PostMapping("/items")
	public ResponseEntity<CartResponse> addItem(
			Principal principal,
			@Valid @RequestBody AddCartItemRequest request) {
		return ResponseEntity.ok(cartService.addItem(principal.getName(), request));
	}

	@PutMapping("/items/{variantId}")
	public ResponseEntity<CartResponse> updateItemQuantity(
			Principal principal,
			@PathVariable Long variantId,
			@Valid @RequestBody UpdateCartItemRequest request) {
		return ResponseEntity.ok(cartService.updateItemQuantity(principal.getName(), variantId, request));
	}

	@DeleteMapping("/items/{variantId}")
	public ResponseEntity<CartResponse> removeItem(Principal principal, @PathVariable Long variantId) {
		return ResponseEntity.ok(cartService.removeItem(principal.getName(), variantId));
	}

	@DeleteMapping
	public ResponseEntity<CartResponse> clearCart(Principal principal) {
		return ResponseEntity.ok(cartService.clearCart(principal.getName()));
	}
}
