package com.badminton.shop.modules.product.repository.projection;

import java.math.BigDecimal;

public interface AdminProductListProjection {
    Long getId();

    String getName();

    String getSlug();

    String getShortDescription();

    String getThumbnailUrl();

    BigDecimal getBasePrice();

    String getBrandName();

    String getCategoryName();

    Boolean getIsActive();

    Boolean getIsDeleted();
}
