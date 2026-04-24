package com.badminton.shop.modules.promotion.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.messaging.dto.PromotionUsageMessage;
import com.badminton.shop.modules.promotion.dto.request.PromotionRequest;
import com.badminton.shop.modules.promotion.dto.response.PromotionApplyResult;
import com.badminton.shop.modules.promotion.dto.response.PromotionResponse;
import com.badminton.shop.modules.promotion.dto.response.PromotionValidationResponse;
import com.badminton.shop.modules.promotion.entity.DiscountType;
import com.badminton.shop.modules.promotion.entity.Promotion;
import com.badminton.shop.modules.promotion.repository.PromotionRepository;
import com.badminton.shop.modules.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private static final String PROMOTION_CODE_CACHE_PREFIX = "promotion:code:";

    private final PromotionRepository promotionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.promotion.redis.ttl-minutes:20}")
    private long promotionCacheTtlMinutes;

    @Override
    public PromotionResponse createPromotion(PromotionRequest request) {
        String normalizedCode = normalizeCode(request.getCode());
        if (promotionRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Promotion code already exists: " + normalizedCode);
        }

        validatePromotionRequest(request);

        Promotion promotion = Promotion.builder()
                .code(normalizedCode)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .maxUsage(request.getMaxUsage())
                .currentUsage(0)
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .build();

        Promotion saved = promotionRepository.save(promotion);
        evictPromotionCache(saved.getCode());
        return toResponse(saved);
    }

    @Override
    public List<PromotionResponse> createPromotions(List<PromotionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Promotion list must not be empty.");
        }

        // Kiểm tra trùng code trong chính request
        Set<String> codesInRequest = new HashSet<>();
        for (PromotionRequest req : requests) {
            String normalized = normalizeCode(req.getCode());
            if (!codesInRequest.add(normalized)) {
                throw new IllegalArgumentException("Duplicate promotion code in request: " + normalized);
            }
        }

        // Kiểm tra trùng với DB
        for (PromotionRequest req : requests) {
            String normalized = normalizeCode(req.getCode());
            if (promotionRepository.existsByCodeIgnoreCase(normalized)) {
                throw new IllegalArgumentException("Promotion code already exists: " + normalized);
            }
            validatePromotionRequest(req);
        }

        List<Promotion> promotions = new ArrayList<>();
        for (PromotionRequest req : requests) {
            promotions.add(Promotion.builder()
                    .code(normalizeCode(req.getCode()))
                    .discountType(req.getDiscountType())
                    .discountValue(req.getDiscountValue())
                    .minOrderValue(req.getMinOrderValue())
                    .maxDiscountAmount(req.getMaxDiscountAmount())
                    .maxUsage(req.getMaxUsage())
                    .currentUsage(0)
                    .startDate(req.getStartDate())
                    .expiryDate(req.getExpiryDate())
                    .isActive(Boolean.TRUE.equals(req.getIsActive()))
                    .build());
        }

        List<Promotion> saved = promotionRepository.saveAll(promotions);
        saved.forEach(p -> evictPromotionCache(p.getCode()));
        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + id));

        String normalizedCode = normalizeCode(request.getCode());
        if (!promotion.getCode().equalsIgnoreCase(normalizedCode)
                && promotionRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Promotion code already exists: " + normalizedCode);
        }

        validatePromotionRequest(request);

        String oldCode = promotion.getCode();
        promotion.setCode(normalizedCode);
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMinOrderValue(request.getMinOrderValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setMaxUsage(request.getMaxUsage());
        promotion.setStartDate(request.getStartDate());
        promotion.setExpiryDate(request.getExpiryDate());
        promotion.setIsActive(Boolean.TRUE.equals(request.getIsActive()));

        Promotion saved = promotionRepository.save(promotion);
        evictPromotionCache(oldCode);
        evictPromotionCache(saved.getCode());
        return toResponse(saved);
    }

    @Override
    public PromotionResponse setPromotionActive(Long id, boolean active) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + id));

        promotion.setIsActive(active);
        Promotion saved = promotionRepository.save(promotion);
        evictPromotionCache(saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionByCode(String code) {
        Promotion promotion = getPromotionByCodeCached(code);
        return toResponse(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getPromotions(int page, int size, Boolean activeOnly) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Promotion> promotionPage = Boolean.TRUE.equals(activeOnly)
            ? promotionRepository.findAllByIsActiveTrue(pageable)
            : promotionRepository.findAll(pageable);
        return promotionPage.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionValidationResponse validatePromotion(String voucherCode, BigDecimal itemsAmount, BigDecimal shippingFee) {
        PromotionApplyResult result = applyPromotionForOrder(voucherCode, itemsAmount, shippingFee);
        return PromotionValidationResponse.builder()
                .voucherCode(result.getVoucherCode())
                .valid(true)
                .message("Voucher applied successfully.")
                .discountAmount(result.getDiscountAmount())
                .finalShippingFee(result.getFinalShippingFee())
                .finalTotalAmount(result.getFinalTotalAmount())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionApplyResult applyPromotionForOrder(String voucherCode, BigDecimal itemsAmount, BigDecimal shippingFee) {
        BigDecimal safeItemsAmount = nullToZero(itemsAmount);
        BigDecimal safeShippingFee = nullToZero(shippingFee);

        if (voucherCode == null || voucherCode.isBlank()) {
            return PromotionApplyResult.builder()
                    .promotion(null)
                    .voucherCode(null)
                    .discountAmount(BigDecimal.ZERO)
                    .finalShippingFee(safeShippingFee)
                    .finalTotalAmount(safeItemsAmount.add(safeShippingFee))
                    .build();
        }

        Promotion promotion = getPromotionByCodeCached(voucherCode);
        validatePromotionForCheckout(promotion, safeItemsAmount);

        BigDecimal discountAmount = calculateDiscountAmount(promotion, safeItemsAmount, safeShippingFee);
        BigDecimal finalShippingFee = promotion.getDiscountType() == DiscountType.FREE_SHIP
                ? BigDecimal.ZERO
                : safeShippingFee;

        BigDecimal finalTotalAmount = safeItemsAmount.add(finalShippingFee).subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        if (finalTotalAmount.signum() < 0) {
            finalTotalAmount = BigDecimal.ZERO;
        }

        return PromotionApplyResult.builder()
                .promotion(promotion)
                .voucherCode(promotion.getCode())
                .discountAmount(discountAmount)
                .finalShippingFee(finalShippingFee)
                .finalTotalAmount(finalTotalAmount)
                .build();
    }

    @Override
    public void publishPromotionUsage(Long promotionId, String orderCode) {
        if (promotionId == null || orderCode == null || orderCode.isBlank()) {
            return;
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROMOTION_EXCHANGE,
                RabbitMQConfig.PROMOTION_USAGE_ROUTING_KEY,
                PromotionUsageMessage.builder()
                        .promotionId(promotionId)
                        .orderCode(orderCode)
                        .usageDelta(1)
                        .build()
        );
    }

    private Promotion getPromotionByCodeCached(String code) {
        String normalizedCode = normalizeCode(code);
        String cacheKey = PROMOTION_CODE_CACHE_PREFIX + normalizedCode;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof PromotionResponse cachedResponse) {
            return promotionRepository.findById(cachedResponse.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with code: " + normalizedCode));
        }

        Promotion promotion = promotionRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with code: " + normalizedCode));

        redisTemplate.opsForValue().set(cacheKey, toResponse(promotion), Duration.ofMinutes(promotionCacheTtlMinutes));
        return promotion;
    }

    private void validatePromotionRequest(PromotionRequest request) {
        if (request.getStartDate().isAfter(request.getExpiryDate())) {
            throw new IllegalArgumentException("Promotion startDate must be before expiryDate");
        }

        if (request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue() > 100.0d) {
            throw new IllegalArgumentException("Percentage discount must be <= 100");
        }

        if (request.getDiscountType() == DiscountType.FREE_SHIP && request.getDiscountValue() != 0.0d) {
            throw new IllegalArgumentException("FREE_SHIP requires discountValue = 0");
        }
    }

    private void validatePromotionForCheckout(Promotion promotion, BigDecimal itemsAmount) {
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(promotion.getIsActive())) {
            throw new IllegalArgumentException("Voucher is inactive.");
        }

        if (promotion.getStartDate().isAfter(now) || promotion.getExpiryDate().isBefore(now)) {
            throw new IllegalArgumentException("Voucher is out of valid time range.");
        }

        if (promotion.getMinOrderValue() != null
                && itemsAmount.compareTo(BigDecimal.valueOf(promotion.getMinOrderValue())) < 0) {
            throw new IllegalArgumentException("Order does not meet minimum value for this voucher.");
        }

        int currentUsage = promotion.getCurrentUsage() == null ? 0 : promotion.getCurrentUsage();
        if (promotion.getMaxUsage() != null && currentUsage >= promotion.getMaxUsage()) {
            throw new IllegalArgumentException("Voucher usage limit reached.");
        }
    }

    private BigDecimal calculateDiscountAmount(Promotion promotion, BigDecimal itemsAmount, BigDecimal shippingFee) {
        BigDecimal discount;
        switch (promotion.getDiscountType()) {
            case PERCENTAGE -> {
                discount = itemsAmount.multiply(BigDecimal.valueOf(promotion.getDiscountValue()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (promotion.getMaxDiscountAmount() != null) {
                    discount = discount.min(BigDecimal.valueOf(promotion.getMaxDiscountAmount()));
                }
            }
            case FIXED_AMOUNT -> discount = BigDecimal.valueOf(promotion.getDiscountValue());
            case FREE_SHIP -> discount = shippingFee;
            default -> discount = BigDecimal.ZERO;
        }

        BigDecimal cap = itemsAmount.add(shippingFee);
        if (discount.compareTo(cap) > 0) {
            discount = cap;
        }
        return discount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void evictPromotionCache(String code) {
        if (code != null && !code.isBlank()) {
            redisTemplate.delete(PROMOTION_CODE_CACHE_PREFIX + code.toUpperCase(Locale.ROOT));
        }
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .code(promotion.getCode())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .minOrderValue(promotion.getMinOrderValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .maxUsage(promotion.getMaxUsage())
                .currentUsage(promotion.getCurrentUsage())
                .startDate(promotion.getStartDate())
                .expiryDate(promotion.getExpiryDate())
                .isActive(promotion.getIsActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + id));
        return toResponse(promotion);
    }

    @Override
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + id));
        promotionRepository.delete(promotion);
        evictPromotionCache(promotion.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> adminGetPromotionStats() {
        long total = promotionRepository.count();
        long active = promotionRepository.countByIsActiveTrue();
        long inactive = total - active;
        // In a real app we might sum usages or find expired. 
        // For now, return basic stats.
        return java.util.Map.of(
            "total", total,
            "active", active,
            "inactive", inactive
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Object> adminGetPromotionUsages(Long id, int page, int size) {
        // Mocking usage response since we don't have a Usage table yet
        return Page.empty();
    }
}
