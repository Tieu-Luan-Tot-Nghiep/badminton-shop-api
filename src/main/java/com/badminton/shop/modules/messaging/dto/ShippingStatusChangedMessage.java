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
public class ShippingStatusChangedMessage implements Serializable {

    private String clientOrderCode;
    private String shippingCode;
    private String status;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
