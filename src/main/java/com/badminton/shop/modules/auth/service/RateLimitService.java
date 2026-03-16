package com.badminton.shop.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final String LOGIN_FAIL_PREFIX = "loginfail:";
    private static final String LOCKOUT_PREFIX = "lockout:";

    /**
     * Kiểm tra giới hạn request (Rate Limit) cho một định danh (IP hoặc Email).
     * 
     * @param identifier Định danh của khách hàng (IP hoặc Email).
     * @param limit      Số lượng request tối đa trong khoảng thời gian.
     * @param seconds    Khoảng thời gian (giây).
     * @return true nếu vượt quá giới hạn.
     */
    public boolean isRateLimited(String identifier, int limit, int seconds) {
        String key = RATE_LIMIT_PREFIX + identifier;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        }

        return count != null && count > limit;
    }

    /**
     * Kiểm tra xem IP/Email có đang trong thời gian chờ (Cooldown) không.
     * 
     * @param identifier Định danh.
     * @param seconds    Thời gian chờ tối thiểu giữa 2 lần gửi.
     * @return true nếu đang trong cooldown.
     */
    public boolean isInCooldown(String identifier, int seconds) {
        String key = RATE_LIMIT_PREFIX + "cooldown:" + identifier;
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.opsForValue().set(key, "1", seconds, TimeUnit.SECONDS);
            return false;
        }

        return true;
    }

    /**
     * Theo dõi số lần đăng nhập sai.
     * 
     * @param identifier Định danh (Username/Email/IP).
     * @return true nếu đạt đến ngưỡng khóa (5 lần).
     */
    public boolean trackLoginFailure(String identifier) {
        String key = LOGIN_FAIL_PREFIX + identifier;
        Long failures = redisTemplate.opsForValue().increment(key);

        if (failures != null && failures == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS); // Tự động reset sau 1 tiếng nếu không đủ 5 lần
        }

        if (failures != null && failures >= 5) {
            lockIdentifier(identifier, 15); // Khóa 15 phút
            redisTemplate.delete(key); // Reset bộ đếm sau khi khóa
            return true;
        }

        return false;
    }

    /**
     * Khóa một định danh.
     * 
     * @param identifier Định danh.
     * @param minutes    Thời gian khóa (phút).
     */
    public void lockIdentifier(String identifier, int minutes) {
        redisTemplate.opsForValue().set(LOCKOUT_PREFIX + identifier, "LOCKED", minutes, TimeUnit.MINUTES);
    }

    /**
     * Kiểm tra xem định danh có đang bị khóa không.
     * 
     * @param identifier Định danh.
     * @return true nếu đang bị khóa.
     */
    public boolean isLocked(String identifier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_PREFIX + identifier));
    }

    /**
     * Xóa dấu đăng nhập sai khi đăng nhập thành công.
     */
    public void resetLoginFailures(String identifier) {
        redisTemplate.delete(LOGIN_FAIL_PREFIX + identifier);
    }
}
