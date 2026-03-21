package com.badminton.shop.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLedgerResponse {
    private Long variantId;
    private String sku;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Page<InventoryLedgerEntryResponse> entries;
}
