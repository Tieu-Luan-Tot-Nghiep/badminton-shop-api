package com.badminton.shop.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInventoryProductResponse {
    private Long id;
    private String name;
    private Integer totalQuantity;

    @Builder.Default
    private List<AdminInventoryVariantResponse> variants = new ArrayList<>();
}
