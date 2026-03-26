package com.badminton.shop.modules.membership.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.membership.dto.response.UserMembershipResponse;
import com.badminton.shop.modules.membership.entity.MembershipTier;
import com.badminton.shop.modules.membership.entity.UserMembership;
import com.badminton.shop.modules.membership.repository.MembershipTierRepository;
import com.badminton.shop.modules.membership.repository.PointHistoryRepository;
import com.badminton.shop.modules.membership.repository.UserMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceImplTest {

    @Mock
    private UserMembershipRepository userMembershipRepository;
    @Mock
    private MembershipTierRepository tierRepository;
    @Mock
    private PointHistoryRepository pointHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MembershipServiceImpl membershipService;

    private User testUser;
    private MembershipTier bronzeTier;
    private MembershipTier silverTier;
    private UserMembership testMembership;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@email.com");

        bronzeTier = new MembershipTier(1L, "BRONZE", 0, BigDecimal.ZERO, "Default tier");
        silverTier = new MembershipTier(2L, "SILVER", 500, BigDecimal.valueOf(5), "Silver discount");

        testMembership = new UserMembership(1L, testUser, bronzeTier, 100, 100);
    }

    @Test
    void getUserMembership_ShouldReturnResponse_WhenExists() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.of(testMembership));

        UserMembershipResponse response = membershipService.getUserMembership(1L);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals("BRONZE", response.tier().name());
        assertEquals(100, response.currentPoints());
    }

    @Test
    void getUserMembership_ShouldThrowException_WhenNotFound() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> membershipService.getUserMembership(1L));
    }

    @Test
    void getUserMembership_ShouldInitMembership_WhenMembershipNotExists() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tierRepository.findLowestTier()).thenReturn(Optional.of(bronzeTier));
        when(userMembershipRepository.save(any(UserMembership.class))).thenReturn(testMembership);

        UserMembershipResponse response = membershipService.getUserMembership(1L);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        verify(userMembershipRepository).save(any(UserMembership.class));
    }

    @Test
    void initMembershipForUser_ShouldCreateNew_WhenNotExists() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(tierRepository.findLowestTier()).thenReturn(Optional.of(bronzeTier));
        when(userMembershipRepository.save(any(UserMembership.class))).thenReturn(testMembership);

        UserMembership result = membershipService.initMembershipForUser(testUser);

        assertNotNull(result);
        verify(userMembershipRepository).save(any(UserMembership.class));
    }

    @Test
    void addPointsFromOrder_ShouldAddPointsAndUpgradeTier_WhenOrderTotalIsHigh() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.of(testMembership));
        when(tierRepository.findEligibleTierByPoints(anyInt())).thenReturn(Optional.of(silverTier));

        // 10,000,000 VND = 1000 points
        membershipService.addPointsFromOrder(1L, new BigDecimal("10000000"), 101L);

        assertEquals(1100, testMembership.getCurrentPoints());
        assertEquals(1100, testMembership.getTotalPoints());
        assertEquals("SILVER", testMembership.getTier().getName());

        verify(userMembershipRepository).save(testMembership);
        verify(pointHistoryRepository).save(any());
    }

    @Test
    void usePoints_ShouldDeductPoints_WhenSufficientPoints() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.of(testMembership));

        boolean result = membershipService.usePoints(1L, 50, "USE_VOUCHER", 99L);

        assertTrue(result);
        assertEquals(50, testMembership.getCurrentPoints());
        verify(userMembershipRepository).save(testMembership);
        verify(pointHistoryRepository).save(any());
    }

    @Test
    void usePoints_ShouldThrowException_WhenInsufficientPoints() {
        when(userMembershipRepository.findByUserId(1L)).thenReturn(Optional.of(testMembership));

        assertThrows(IllegalArgumentException.class, () -> membershipService.usePoints(1L, 200, "USE_VOUCHER", 99L));
    }
}
