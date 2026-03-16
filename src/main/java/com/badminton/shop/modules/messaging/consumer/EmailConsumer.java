package com.badminton.shop.modules.messaging.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.EmailMessage;
import com.badminton.shop.utils.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_VERIFICATION_QUEUE)
    public void consumeEmailVerification(EmailMessage message) {
        log.info("Received {} message for: {}", message.getType(), message.getEmail());
        try {
            if (message.getType() == EmailMessage.EmailType.FORGOT_PASSWORD) {
                emailService.sendForgotPasswordEmail(message.getEmail(), message.getToken());
            } else {
                emailService.sendVerificationEmail(message.getEmail(), message.getToken());
            }
            log.info("Email sent successfully to: {}", message.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", message.getEmail(), e);
        }
    }
}
