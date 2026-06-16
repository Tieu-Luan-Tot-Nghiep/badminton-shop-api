package com.badminton.shop.modules.messaging.service;

import com.badminton.shop.modules.messaging.dto.FcmNotificationMessage;

public interface FcmService {

    /**
     * Gửi push notification trực tiếp đến một FCM token.
     *
     * @param message DTO chứa fcmToken, title, body và data payload
     */
    void send(FcmNotificationMessage message);

    /**
     * Gửi push notification bất đồng bộ qua RabbitMQ queue.
     * Dùng khi không cần đợi kết quả gửi FCM trong luồng chính.
     *
     * @param message DTO chứa fcmToken, title, body và data payload
     */
    void sendAsync(FcmNotificationMessage message);
}
