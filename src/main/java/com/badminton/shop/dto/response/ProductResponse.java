package com.badminton.shop.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal effectivePrice;
    private Integer stockQuantity;
    private String brand;
    private String imageUrl;
    private boolean active;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
}
