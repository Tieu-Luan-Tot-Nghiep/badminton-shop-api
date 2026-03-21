package com.badminton.shop.modules.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StocktakeAdjustmentRequest {

    @NotNull(message = "variantId is required")
    private Long variantId;

    @NotNull(message = "actualAvailableQuantity is required")
    @Min(value = 0, message = "actualAvailableQuantity must be >= 0")
    private Integer actualAvailableQuantity;

    private String note;
}
