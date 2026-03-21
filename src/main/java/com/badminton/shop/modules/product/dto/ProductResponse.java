package com.badminton.shop.modules.product.dto;

import com.badminton.shop.modules.review.dto.response.ReviewResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String thumbnailUrl;
    private BigDecimal basePrice;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin liên kết
    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    private Long brandId;
    private String brandName;
    private String brandSlug;

    private List<ProductImageResponse> productImages;
    private List<ProductVariantResponse> productVariants;
    private List<ReviewResponse> latestReviews;
}
