package com.badminton.shop.service;

import com.badminton.shop.dto.request.CartItemRequest;
import com.badminton.shop.dto.response.CartResponse;

public interface CartService {

    CartResponse getCartByUserId(Long userId);

    CartResponse addItemToCart(Long userId, CartItemRequest request);

    CartResponse updateCartItemQuantity(Long userId, Long productId, Integer quantity);

    CartResponse removeItemFromCart(Long userId, Long productId);

    void clearCart(Long userId);
}
