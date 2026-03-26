package com.badminton.shop.modules.membership.dto.response;

import java.math.BigDecimal;

public record MembershipTierResponse(
    Long id,
    String name,
    Integer minPoints,
    BigDecimal discountPercent,
    String benefits
) {}
