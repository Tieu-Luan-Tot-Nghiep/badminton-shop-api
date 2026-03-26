package com.badminton.shop.modules.shipping.dto.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNItem {
    private String name;
    private String code;
    private Integer quantity;
    private Integer price;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer weight;
}
