package com.badminton.shop.dto.response;

import com.badminton.shop.enums.PaymentMethod;
import com.badminton.shop.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private String transactionCode;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
