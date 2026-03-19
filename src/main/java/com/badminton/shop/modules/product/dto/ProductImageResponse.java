package com.badminton.shop.modules.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private String color;
    private Boolean isMain;
}
