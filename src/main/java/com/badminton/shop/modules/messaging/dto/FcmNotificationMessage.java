package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FcmNotificationMessage implements Serializable {

    /** FCM token của device cần gửi (lấy từ User.fcmToken) */
    private String fcmToken;

    /** Tiêu đề notification */
    private String title;

    /** Nội dung notification */
    private String body;

    /** Data payload tuỳ chọn (screen, orderCode, ...) */
    private Map<String, String> data;
}
