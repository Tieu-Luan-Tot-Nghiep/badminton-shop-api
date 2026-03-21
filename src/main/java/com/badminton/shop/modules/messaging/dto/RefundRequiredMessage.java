package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequiredMessage implements Serializable {
    private String orderCode;
    private String reason;
    private String paymentMethod;
    private String customerEmail;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
