package com.badminton.shop.modules.order.service;

import com.badminton.shop.modules.order.dto.request.AddCartItemRequest;
import com.badminton.shop.modules.order.dto.request.UpdateCartItemRequest;
import com.badminton.shop.modules.order.dto.response.CartResponse;

public interface CartService {

    CartResponse getMyCart(String principalName);

    CartResponse addItem(String principalName, AddCartItemRequest request);

    CartResponse updateItemQuantity(String principalName, Long variantId, UpdateCartItemRequest request);

    CartResponse removeItem(String principalName, Long variantId);

    CartResponse clearCart(String principalName);
}
