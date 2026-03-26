package com.badminton.shop.modules.membership.service;

import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.membership.dto.response.PointHistoryResponse;
import com.badminton.shop.modules.membership.dto.response.UserMembershipResponse;
import com.badminton.shop.modules.membership.entity.UserMembership;

import java.math.BigDecimal;
import java.util.List;

public interface MembershipService {
    UserMembershipResponse getUserMembership(Long userId);
    List<PointHistoryResponse> getUserPointHistory(Long userId);
    UserMembership initMembershipForUser(User user);
    void addPointsFromOrder(Long userId, BigDecimal orderTotal, Long orderId);
    boolean rollbackPointsFromOrder(Long userId, Long orderId, String reason);
    boolean usePoints(Long userId, Integer pointsToUse, String reason, Long referenceId);
    BigDecimal getDiscountPercentage(Long userId);
}
