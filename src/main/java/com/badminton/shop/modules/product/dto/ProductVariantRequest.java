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

    private String weight;

    private String gripSize;

    private String stiffness;

    private String balancePoint;

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

    @Min(value = 1, message = "shippingWeightGrams phải lớn hơn 0")
    private Integer shippingWeightGrams;

    @Min(value = 1, message = "shippingLengthCm phải lớn hơn 0")
    private Integer shippingLengthCm;

    @Min(value = 1, message = "shippingWidthCm phải lớn hơn 0")
    private Integer shippingWidthCm;

    @Min(value = 1, message = "shippingHeightCm phải lớn hơn 0")
    private Integer shippingHeightCm;
}
