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
public class PromotionUsageMessage implements Serializable {
    private Long promotionId;
    private String orderCode;

    @Builder.Default
    private Integer usageDelta = 1;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
