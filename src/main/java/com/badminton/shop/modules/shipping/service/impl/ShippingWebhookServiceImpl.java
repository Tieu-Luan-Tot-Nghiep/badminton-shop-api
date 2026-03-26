package com.badminton.shop.modules.shipping.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.ShippingStatusChangedMessage;
import com.badminton.shop.modules.shipping.config.GHNProperties;
import com.badminton.shop.modules.shipping.dto.request.ShippingWebhookRequest;
import com.badminton.shop.modules.shipping.service.ShippingWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingWebhookServiceImpl implements ShippingWebhookService {

    private final RabbitTemplate rabbitTemplate;
    private final GHNProperties ghnProperties;

    @Override
    public void handleWebhook(ShippingWebhookRequest request) {
        if (request == null || !hasText(request.getStatus())) {
            return;
        }

        if (ghnProperties.getShopId() != null && request.getShopId() != null
                && !ghnProperties.getShopId().equals(request.getShopId())) {
            log.warn("[shipping-webhook] ShopID mismatch: expected={}, actual={}", ghnProperties.getShopId(), request.getShopId());
            return;
        }

        ShippingStatusChangedMessage message = ShippingStatusChangedMessage.builder()
                .clientOrderCode(emptyToNull(request.getClientOrderCode()))
                .shippingCode(emptyToNull(request.getOrderCode()))
                .status(request.getStatus())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_SHIPPING_STATUS_ROUTING_KEY,
                message
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
