package com.badminton.shop.modules.promotion.scheduler;

import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.messaging.dto.FcmNotificationMessage;
import com.badminton.shop.modules.messaging.service.FcmService;
import com.badminton.shop.modules.promotion.entity.Promotion;
import com.badminton.shop.modules.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Scheduler gửi FCM push notification về promotion:
 * 1. Voucher sắp hết hạn trong 24 giờ tới  — chạy mỗi 6 giờ
 * 2. Flash sale / promotion sắp bắt đầu trong 1 giờ tới — chạy mỗi 30 phút
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionNotificationScheduler {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final int USER_BATCH_SIZE = 200;

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    /**
     * Mỗi 6 giờ: tìm voucher active sắp hết hạn trong 24h và notify toàn bộ user có fcmToken.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void notifyExpiringVouchers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(24);

        List<Promotion> expiring = promotionRepository.findActiveExpiringSoon(now, deadline);
        if (expiring.isEmpty()) {
            return;
        }

        log.info("[PromotionScheduler] Tìm thấy {} voucher sắp hết hạn trong 24h", expiring.size());

        // Lấy danh sách user có FCM token theo batch
        int page = 0;
        List<User> batch;
        do {
            batch = userRepository.findAll(PageRequest.of(page++, USER_BATCH_SIZE)).getContent()
                    .stream()
                    .filter(u -> u.getFcmToken() != null && !u.getFcmToken().isBlank())
                    .toList();

            for (User user : batch) {
                for (Promotion p : expiring) {
                    String expireTime = p.getExpiryDate().format(DISPLAY_FMT);
                    sendPromotion(user.getFcmToken(),
                            "⏰ Voucher sắp hết hạn!",
                            "Voucher " + p.getCode() + " sẽ hết hạn lúc " + expireTime + ". Dùng ngay kẻo lỡ!",
                            "VOUCHER_EXPIRING",
                            p.getCode());
                }
            }
        } while (batch.size() == USER_BATCH_SIZE);

        log.info("[PromotionScheduler] Đã gửi notification voucher sắp hết hạn xong.");
    }

    /**
     * Mỗi 30 phút: tìm promotion sắp bắt đầu trong 1h tới và notify user.
     */
    @Scheduled(cron = "0 0/30 * * * *")
    public void notifyUpcomingFlashSale() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusHours(1);

        List<Promotion> upcoming = promotionRepository.findStartingSoon(now, soon);
        if (upcoming.isEmpty()) {
            return;
        }

        log.info("[PromotionScheduler] Tìm thấy {} promotion sắp bắt đầu trong 1h", upcoming.size());

        int page = 0;
        List<User> batch;
        do {
            batch = userRepository.findAll(PageRequest.of(page++, USER_BATCH_SIZE)).getContent()
                    .stream()
                    .filter(u -> u.getFcmToken() != null && !u.getFcmToken().isBlank())
                    .toList();

            for (User user : batch) {
                for (Promotion p : upcoming) {
                    String startTime = p.getStartDate().format(DISPLAY_FMT);
                    String discountDesc = buildDiscountDescription(p);
                    sendPromotion(user.getFcmToken(),
                            "🔥 Flash Sale sắp bắt đầu!",
                            "Khuyến mãi " + p.getCode() + " (" + discountDesc + ") sẽ mở lúc " + startTime + ". Chuẩn bị ngay!",
                            "FLASH_SALE_UPCOMING",
                            p.getCode());
                }
            }
        } while (batch.size() == USER_BATCH_SIZE);

        log.info("[PromotionScheduler] Đã gửi notification flash sale sắp bắt đầu xong.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void sendPromotion(String fcmToken, String title, String body, String screen, String promoCode) {
        try {
            fcmService.sendAsync(FcmNotificationMessage.builder()
                    .fcmToken(fcmToken)
                    .title(title)
                    .body(body)
                    .data(Map.of("screen", screen, "promoCode", promoCode))
                    .build());
        } catch (Exception e) {
            log.warn("[PromotionScheduler] Không thể gửi FCM. promoCode={}: {}", promoCode, e.getMessage());
        }
    }

    private String buildDiscountDescription(Promotion p) {
        return switch (p.getDiscountType()) {
            case PERCENTAGE -> (int) p.getDiscountValue().doubleValue() + "% giảm giá";
            case FIXED_AMOUNT -> formatCurrency(p.getDiscountValue()) + " giảm trực tiếp";
            case FREE_SHIP -> "Miễn phí vận chuyển";
        };
    }

    private String formatCurrency(Double value) {
        if (value == null) return "0đ";
        long v = value.longValue();
        if (v >= 1_000_000) return (v / 1_000_000) + "M đ";
        if (v >= 1_000) return (v / 1_000) + "K đ";
        return v + "đ";
    }
}
