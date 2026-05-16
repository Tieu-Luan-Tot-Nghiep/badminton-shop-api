package com.badminton.shop.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInventoryVariantResponse {
    private Long id;
    private String name;
    private Integer quantity;
}
