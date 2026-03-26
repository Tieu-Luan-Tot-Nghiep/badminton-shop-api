package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderConfirmationEmailMessage implements Serializable {
    private String email;
    private String orderCode;
    private String customerName;
    private Double totalAmount;
    private String paymentMethod;
}
