package com.badminton.shop.modules.promotion.service.impl;

import com.badminton.shop.modules.promotion.dto.request.PromotionRequest;
import com.badminton.shop.modules.promotion.dto.response.PromotionApplyResult;
import com.badminton.shop.modules.promotion.entity.DiscountType;
import com.badminton.shop.modules.promotion.entity.Promotion;
import com.badminton.shop.modules.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    @BeforeEach
    void setUp() {
                lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(promotionService, "promotionCacheTtlMinutes", 20L);
    }

    @Test
    void createPromotion_ShouldThrow_WhenStartDateAfterExpiryDate() {
        PromotionRequest request = PromotionRequest.builder()
                .code("late-sale")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(100000.0)
                .startDate(LocalDateTime.now().plusDays(2))
                .expiryDate(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();

        when(promotionRepository.existsByCodeIgnoreCase("LATE-SALE")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> promotionService.createPromotion(request));

        assertEquals("Promotion startDate must be before expiryDate", ex.getMessage());
    }

    @Test
    void createPromotion_ShouldThrow_WhenPercentageDiscountGreaterThan100() {
        PromotionRequest request = PromotionRequest.builder()
                .code("big-percent")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(120.0)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(10))
                .isActive(true)
                .build();

        when(promotionRepository.existsByCodeIgnoreCase("BIG-PERCENT")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> promotionService.createPromotion(request));

        assertEquals("Percentage discount must be <= 100", ex.getMessage());
    }

    @Test
    void applyPromotionForOrder_ShouldCalculatePercentageDiscount_WithMaxCap() {
        Promotion promotion = Promotion.builder()
                .id(1L)
                .code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(20.0)
                .maxDiscountAmount(50.0)
                .currentUsage(0)
                .maxUsage(100)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();

        when(valueOperations.get("promotion:code:SAVE20")).thenReturn(null);
        when(promotionRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(promotion));

        PromotionApplyResult result = promotionService.applyPromotionForOrder(
                "save20",
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(30)
        );

        assertEquals(new BigDecimal("50.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("30"), result.getFinalShippingFee());
        assertEquals(new BigDecimal("380.00"), result.getFinalTotalAmount());
        assertEquals("SAVE20", result.getVoucherCode());
    }

    @Test
    void applyPromotionForOrder_ShouldApplyFreeShip() {
        Promotion promotion = Promotion.builder()
                .id(2L)
                .code("FREESHIP")
                .discountType(DiscountType.FREE_SHIP)
                .discountValue(0.0)
                .currentUsage(5)
                .maxUsage(100)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();

        when(valueOperations.get("promotion:code:FREESHIP")).thenReturn(null);
        when(promotionRepository.findByCodeIgnoreCase("FREESHIP")).thenReturn(Optional.of(promotion));

        PromotionApplyResult result = promotionService.applyPromotionForOrder(
                "freeship",
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(30)
        );

        assertEquals(new BigDecimal("30.00"), result.getDiscountAmount());
        assertEquals(BigDecimal.ZERO, result.getFinalShippingFee());
        assertEquals(new BigDecimal("220.00"), result.getFinalTotalAmount());
    }

    @Test
    void applyPromotionForOrder_ShouldThrow_WhenUsageLimitReached() {
        Promotion promotion = Promotion.builder()
                .id(3L)
                .code("LIMITED")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(20.0)
                .currentUsage(10)
                .maxUsage(10)
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();

        when(valueOperations.get("promotion:code:LIMITED")).thenReturn(null);
        when(promotionRepository.findByCodeIgnoreCase("LIMITED")).thenReturn(Optional.of(promotion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> promotionService.applyPromotionForOrder("limited", BigDecimal.valueOf(200), BigDecimal.valueOf(20)));

        assertEquals("Voucher usage limit reached.", ex.getMessage());
    }

    @Test
    void applyPromotionForOrder_ShouldReturnNoDiscount_WhenVoucherCodeIsBlank() {
        PromotionApplyResult result = promotionService.applyPromotionForOrder(
                "   ",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(30)
        );

        assertEquals(BigDecimal.ZERO, result.getDiscountAmount());
        assertEquals(new BigDecimal("30"), result.getFinalShippingFee());
        assertEquals(new BigDecimal("130"), result.getFinalTotalAmount());
    }
}
