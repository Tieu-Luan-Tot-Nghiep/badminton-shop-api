package com.badminton.shop.modules.messaging.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.FcmNotificationMessage;
import com.badminton.shop.modules.messaging.service.FcmService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmServiceImpl implements FcmService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(FcmNotificationMessage message) {
        if (message == null || isBlank(message.getFcmToken())) {
            log.debug("[FCM] Bỏ qua: fcmToken rỗng hoặc null");
            return;
        }

        try {
            Message.Builder builder = Message.builder()
                    .setToken(message.getFcmToken())
                    .setNotification(
                            Notification.builder()
                                    .setTitle(message.getTitle())
                                    .setBody(message.getBody())
                                    .build()
                    );

            // Thêm data payload nếu có
            Map<String, String> data = message.getData();
            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.info("[FCM] Gửi thành công. messageId={}, title={}", response, message.getTitle());
        } catch (Exception e) {
            log.error("[FCM] Gửi thất bại. token={}, title={}, error={}",
                    mask(message.getFcmToken()), message.getTitle(), e.getMessage());
        }
    }

    @Override
    public void sendAsync(FcmNotificationMessage message) {
        if (message == null || isBlank(message.getFcmToken())) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.FCM_EXCHANGE,
                    RabbitMQConfig.FCM_NOTIFICATION_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            log.error("[FCM] Không thể đẩy vào queue. error={}", e.getMessage());
            // Fallback: gửi trực tiếp nếu queue không khả dụng
            send(message);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Che bớt token khi log để tránh lộ thông tin nhạy cảm */
    private String mask(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 6) + "***" + token.substring(token.length() - 4);
    }
}
