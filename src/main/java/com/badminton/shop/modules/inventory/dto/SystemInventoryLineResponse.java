package com.badminton.shop.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInventoryLineResponse {
    private Long variantId;
    private Integer quantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
}
