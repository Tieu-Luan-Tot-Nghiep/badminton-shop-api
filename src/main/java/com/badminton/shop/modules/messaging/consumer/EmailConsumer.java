package com.badminton.shop.modules.messaging.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.EmailMessage;
import com.badminton.shop.modules.messaging.dto.OrderCancellationEmailMessage;
import com.badminton.shop.modules.messaging.dto.OrderConfirmationEmailMessage;
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

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLATION_EMAIL_QUEUE)
    public void consumeOrderCancellationEmail(OrderCancellationEmailMessage message) {
        log.info("Received order cancellation email message for: {}, orderCode: {}", message.getEmail(), message.getOrderCode());
        try {
            emailService.sendOrderCancellationEmail(message.getEmail(), message.getOrderCode(), message.getReason());
            log.info("Order cancellation email sent successfully to: {}", message.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancellation email to: {}", message.getEmail(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMATION_EMAIL_QUEUE)
    public void consumeOrderConfirmationEmail(OrderConfirmationEmailMessage message) {
        log.info("Received order confirmation email message for: {}, orderCode: {}", message.getEmail(), message.getOrderCode());
        try {
            emailService.sendOrderConfirmationEmail(
                message.getEmail(),
                message.getOrderCode(),
                message.getCustomerName(),
                message.getTotalAmount(),
                message.getPaymentMethod()
            );
            log.info("Order confirmation email sent successfully to: {}", message.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {}", message.getEmail(), e);
        }
    }
}
