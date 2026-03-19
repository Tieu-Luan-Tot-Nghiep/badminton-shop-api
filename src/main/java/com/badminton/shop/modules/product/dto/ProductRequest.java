package com.badminton.shop.modules.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 500, message = "Tên sản phẩm không được vượt quá 500 ký tự")
    private String name;

    private String shortDescription;

    private String description;

    @DecimalMin(value = "0", message = "Giá cơ bản phải lớn hơn hoặc bằng 0")
    private BigDecimal basePrice;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    @NotNull(message = "Thương hiệu không được để trống")
    private Long brandId;

}
