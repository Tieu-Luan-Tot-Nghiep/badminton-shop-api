package com.badminton.shop.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Giá khuyến mãi phải lớn hơn hoặc bằng 0")
    private BigDecimal salePrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @Size(max = 100, message = "Tên thương hiệu không được vượt quá 100 ký tự")
    private String brand;

    @Size(max = 500, message = "URL ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private Boolean active;
}
