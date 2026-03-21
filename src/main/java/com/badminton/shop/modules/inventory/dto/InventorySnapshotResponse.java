package com.badminton.shop.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySnapshotResponse {
    private Long variantId;
    private String sku;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer lowStockThreshold;
}
