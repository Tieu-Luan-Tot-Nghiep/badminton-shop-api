package com.badminton.shop.modules.promotion.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.PromotionUsageMessage;
import com.badminton.shop.modules.promotion.entity.Promotion;
import com.badminton.shop.modules.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionUsageConsumer {

    private static final String USAGE_EVENT_KEY_PREFIX = "promotion:usage:event:";
    private static final String PROMOTION_CODE_CACHE_PREFIX = "promotion:code:";

    private final PromotionRepository promotionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.PROMOTION_USAGE_QUEUE)
    public void consumePromotionUsage(PromotionUsageMessage message) {
        if (message == null || message.getPromotionId() == null || message.getOrderCode() == null) {
            return;
        }

        String idempotencyKey = USAGE_EVENT_KEY_PREFIX + message.getOrderCode();
        Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, 1, Duration.ofDays(3));
        if (Boolean.FALSE.equals(firstTime)) {
            return;
        }

        Promotion promotion = promotionRepository.findById(message.getPromotionId()).orElse(null);
        if (promotion == null) {
            return;
        }

        int delta = message.getUsageDelta() == null ? 1 : Math.max(0, message.getUsageDelta());
        int currentUsage = promotion.getCurrentUsage() == null ? 0 : promotion.getCurrentUsage();
        int nextUsage = currentUsage + delta;

        if (promotion.getMaxUsage() != null) {
            nextUsage = Math.min(nextUsage, promotion.getMaxUsage());
        }

        promotion.setCurrentUsage(nextUsage);
        promotionRepository.save(promotion);

        redisTemplate.delete(PROMOTION_CODE_CACHE_PREFIX + promotion.getCode().toUpperCase());

        log.info("Promotion usage updated async. code={}, currentUsage={}", promotion.getCode(), nextUsage);
    }
}
