package com.badminton.shop.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageRequest {
    @NotBlank(message = "Màu sắc của ảnh không được để trống")
    private String color;

    @Builder.Default
    private Boolean isMain = false;
}
