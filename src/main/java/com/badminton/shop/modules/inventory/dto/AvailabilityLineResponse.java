package com.badminton.shop.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityLineResponse {
    private Long variantId;
    private Integer requestedQuantity;
    private Integer availableQuantity;
    private boolean available;
}
