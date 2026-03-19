package com.badminton.shop.modules.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {
    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotBlank(message = "Size không được để trống")
    private String size;

    @NotBlank(message = "Màu không được để trống")
    private String color;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private Double price;

    @NotNull(message = "Số lượng tồn không được để trống")
    @Min(value = 0, message = "Số lượng tồn phải lớn hơn hoặc bằng 0")
    private Integer stock;
}
