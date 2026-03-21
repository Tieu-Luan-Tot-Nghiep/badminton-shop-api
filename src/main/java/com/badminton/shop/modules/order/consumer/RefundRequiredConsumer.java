package com.badminton.shop.modules.order.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.RefundRequiredMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefundRequiredConsumer {

    @RabbitListener(queues = RabbitMQConfig.REFUND_REQUIRED_QUEUE)
    public void consumeRefundRequired(RefundRequiredMessage message) {
        if (message == null || message.getOrderCode() == null) {
            return;
        }

        log.warn("[refund-required] orderCode={}, paymentMethod={}, customerEmail={}, reason={}",
                message.getOrderCode(), message.getPaymentMethod(), message.getCustomerEmail(), message.getReason());
    }
}
