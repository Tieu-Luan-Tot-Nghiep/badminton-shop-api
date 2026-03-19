package com.badminton.shop.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long userId;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private Integer totalItems;
    private BigDecimal totalAmount;
    private LocalDateTime updatedAt;
}
