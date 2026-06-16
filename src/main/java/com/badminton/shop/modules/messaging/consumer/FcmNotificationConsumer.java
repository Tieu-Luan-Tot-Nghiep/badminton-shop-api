package com.badminton.shop.modules.messaging.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.FcmNotificationMessage;
import com.badminton.shop.modules.messaging.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmNotificationConsumer {

    private final FcmService fcmService;

    @RabbitListener(queues = RabbitMQConfig.FCM_NOTIFICATION_QUEUE)
    public void consume(FcmNotificationMessage message) {
        if (message == null) {
            return;
        }
        log.info("[FCM Consumer] Nhận notification: title={}", message.getTitle());
        fcmService.send(message);
    }
}
