package com.badminton.shop.modules.membership.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.membership.dto.response.MembershipTierResponse;
import com.badminton.shop.modules.membership.dto.response.PointHistoryResponse;
import com.badminton.shop.modules.membership.dto.response.UserMembershipResponse;
import com.badminton.shop.modules.membership.entity.MembershipTier;
import com.badminton.shop.modules.membership.entity.PointHistory;
import com.badminton.shop.modules.membership.entity.UserMembership;
import com.badminton.shop.modules.membership.repository.MembershipTierRepository;
import com.badminton.shop.modules.membership.repository.PointHistoryRepository;
import com.badminton.shop.modules.membership.repository.UserMembershipRepository;
import com.badminton.shop.modules.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipServiceImpl implements MembershipService {

    private final UserMembershipRepository userMembershipRepository;
    private final MembershipTierRepository tierRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    // 10,000 VND = 1 point
    private static final BigDecimal POINT_CONVERSION_RATE = new BigDecimal("10000");
        private static final String REASON_EARNED_FROM_ORDER = "EARNED_FROM_ORDER";
        private static final String REASON_ROLLBACK_ORDER_CANCELED = "ROLLBACK_ORDER_CANCELED";
        private static final String REASON_ROLLBACK_ORDER_REFUNDED = "ROLLBACK_ORDER_REFUNDED";

    @Override
        @Transactional
    public UserMembershipResponse getUserMembership(Long userId) {
        UserMembership membership = userMembershipRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    return initMembershipForUser(user);
                });

        return mapToResponse(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getUserPointHistory(Long userId) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(history -> new PointHistoryResponse(
                        history.getId(),
                        history.getPoints(),
                        history.getReason(),
                        history.getReferenceId(),
                        history.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public UserMembership initMembershipForUser(User user) {
        if (userMembershipRepository.findByUserId(user.getId()).isPresent()) {
            return userMembershipRepository.findByUserId(user.getId()).get();
        }

        MembershipTier lowestTier = tierRepository.findLowestTier()
                .orElseGet(() -> tierRepository.save(MembershipTier.builder()
                        .name("BRONZE")
                        .minPoints(0)
                        .discountPercent(BigDecimal.ZERO)
                        .benefits("Default tier")
                        .build()));

        UserMembership newMembership = UserMembership.builder()
                .user(user)
                .tier(lowestTier)
                .currentPoints(0)
                .totalPoints(0)
                .build();

        return userMembershipRepository.save(newMembership);
    }

    @Override
    @Transactional
    public void addPointsFromOrder(Long userId, BigDecimal orderTotal, Long orderId) {
                if (orderId != null && pointHistoryRepository.existsByUserIdAndReferenceIdAndReason(userId, orderId, REASON_EARNED_FROM_ORDER)) {
                        return;
                }

        UserMembership membership = userMembershipRepository.findByUserId(userId)
                .orElseGet(() -> initMembershipForUser(userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId))));

        int pointsEarned = orderTotal.divideToIntegralValue(POINT_CONVERSION_RATE).intValue();

        if (pointsEarned > 0) {
            membership.setCurrentPoints(membership.getCurrentPoints() + pointsEarned);
            membership.setTotalPoints(membership.getTotalPoints() + pointsEarned);

            // Update Tier if applicable
            tierRepository.findEligibleTierByPoints(membership.getTotalPoints())
                    .ifPresent(membership::setTier);

            userMembershipRepository.save(membership);

            // Save history
            pointHistoryRepository.save(PointHistory.builder()
                    .user(membership.getUser())
                    .points(pointsEarned)
                                        .reason(REASON_EARNED_FROM_ORDER)
                    .referenceId(orderId)
                    .build());
        }
    }

        @Override
        @Transactional
        public boolean rollbackPointsFromOrder(Long userId, Long orderId, String reason) {
                if (orderId == null) {
                        return false;
                }

                UserMembership membership = userMembershipRepository.findByUserId(userId)
                                .orElse(null);
                if (membership == null) {
                        return false;
                }

                int earned = pointHistoryRepository.sumPointsByUserIdAndReferenceIdAndReason(
                                userId,
                                orderId,
                                REASON_EARNED_FROM_ORDER
                );
                if (earned <= 0) {
                        return false;
                }

                int rolledBack = -pointHistoryRepository.sumPointsByUserIdAndReferenceIdAndReasons(
                                userId,
                                orderId,
                                List.of(REASON_ROLLBACK_ORDER_CANCELED, REASON_ROLLBACK_ORDER_REFUNDED)
                );

                int pointsToRollback = earned - rolledBack;
                if (pointsToRollback <= 0) {
                        return false;
                }

                membership.setCurrentPoints(Math.max(0, membership.getCurrentPoints() - pointsToRollback));
                membership.setTotalPoints(Math.max(0, membership.getTotalPoints() - pointsToRollback));
                tierRepository.findEligibleTierByPoints(membership.getTotalPoints())
                                .ifPresent(membership::setTier);
                userMembershipRepository.save(membership);

                String rollbackReason = REASON_ROLLBACK_ORDER_CANCELED;
                if (reason != null && reason.toLowerCase().contains("refund")) {
                        rollbackReason = REASON_ROLLBACK_ORDER_REFUNDED;
                }

                pointHistoryRepository.save(PointHistory.builder()
                                .user(membership.getUser())
                                .points(-pointsToRollback)
                                .reason(rollbackReason)
                                .referenceId(orderId)
                                .build());

                return true;
        }

    @Override
    @Transactional
    public boolean usePoints(Long userId, Integer pointsToUse, String reason, Long referenceId) {
        UserMembership membership = userMembershipRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + userId));

        if (membership.getCurrentPoints() < pointsToUse) {
            throw new IllegalArgumentException("Not enough points");
        }

        membership.setCurrentPoints(membership.getCurrentPoints() - pointsToUse);
        userMembershipRepository.save(membership);

        pointHistoryRepository.save(PointHistory.builder()
                .user(membership.getUser())
                .points(-pointsToUse)
                .reason(reason)
                .referenceId(referenceId)
                .build());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDiscountPercentage(Long userId) {
        return userMembershipRepository.findByUserId(userId)
                .map(m -> m.getTier().getDiscountPercent())
                .orElse(BigDecimal.ZERO);
    }

    private UserMembershipResponse mapToResponse(UserMembership membership) {
        MembershipTier tier = membership.getTier();
        MembershipTierResponse tierResponse = new MembershipTierResponse(
                tier.getId(),
                tier.getName(),
                tier.getMinPoints(),
                tier.getDiscountPercent(),
                tier.getBenefits()
        );

                Integer pointsToNextTier = 0;
                String nextTierName = null;
                var nextTier = tierRepository.findFirstByMinPointsGreaterThanOrderByMinPointsAsc(membership.getTotalPoints());
                if (nextTier.isPresent()) {
                        pointsToNextTier = Math.max(0, nextTier.get().getMinPoints() - membership.getTotalPoints());
                        nextTierName = nextTier.get().getName();
                }

        return new UserMembershipResponse(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                tierResponse,
                membership.getCurrentPoints(),
                membership.getTotalPoints(),
                pointsToNextTier,
                nextTierName
        );
    }
}
