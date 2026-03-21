package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements Serializable {

    private String orderCode;
    private String reason;
    private String cancelledBy;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Builder.Default
    private List<CancelledItem> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledItem implements Serializable {
        private Long variantId;
        private Integer quantity;
    }
}
