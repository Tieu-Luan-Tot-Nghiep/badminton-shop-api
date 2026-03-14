package com.badminton.shop.dto.response;

import com.badminton.shop.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private String userEmail;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String receiverPhone;
    private String receiverName;
    private String note;
    private List<OrderItemResponse> orderItems;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
