package com.badminton.shop.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {
    private Long wishlistId;
    private Long productId;
    private String productName;
    private String slug;
    private String thumbnailUrl;
    private BigDecimal basePrice;
    private String brandName;
    private String categoryName;
    private LocalDateTime addedAt;
}
