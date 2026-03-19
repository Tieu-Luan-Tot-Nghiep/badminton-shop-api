package com.badminton.shop.modules.order.dto.request;

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
public class AddCartItemRequest {

    @NotNull(message = "variantId không được để trống")
    private Long variantId;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
    private Integer quantity;
}
