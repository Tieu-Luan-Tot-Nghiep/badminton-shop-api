package com.badminton.shop.modules.membership.dto.response;

public record UserMembershipResponse(
    Long userId,
    String email,
    MembershipTierResponse tier,
    Integer currentPoints,
    Integer totalPoints,
    Integer pointsToNextTier,
    String nextTierName
) {}
