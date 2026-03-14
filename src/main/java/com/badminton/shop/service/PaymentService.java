package com.badminton.shop.service;

import com.badminton.shop.dto.response.PaymentResponse;
import com.badminton.shop.enums.PaymentMethod;

public interface PaymentService {

    PaymentResponse getPaymentByOrderId(Long orderId);

    PaymentResponse confirmPayment(Long orderId, String transactionCode);

    PaymentResponse processPayment(Long orderId, PaymentMethod method);
}
