package com.badminton.shop.modules.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
}
