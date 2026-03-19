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
public class CartSyncMessage implements Serializable {

    private Long userId;

    @Builder.Default
    private List<CartSyncItem> items = new ArrayList<>();

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartSyncItem implements Serializable {
        private Long variantId;
        private Integer quantity;
    }
}
