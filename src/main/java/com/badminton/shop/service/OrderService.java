package com.badminton.shop.service;

import com.badminton.shop.dto.request.OrderRequest;
import com.badminton.shop.dto.response.OrderResponse;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.enums.OrderStatus;

public interface OrderService {

    OrderResponse createOrder(Long userId, OrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByCode(String orderCode);

    PageResponse<OrderResponse> getOrdersByUser(Long userId, int page, int size);

    PageResponse<OrderResponse> getAllOrders(int page, int size, OrderStatus status);

    OrderResponse updateOrderStatus(Long id, OrderStatus status);

    void cancelOrder(Long id, Long userId);
}
