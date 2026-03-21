package com.badminton.shop.modules.inventory.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.inventory.dto.InventoryLineRequest;
import com.badminton.shop.modules.inventory.dto.SystemInventoryRequest;
import com.badminton.shop.modules.inventory.service.InventoryService;
import com.badminton.shop.modules.messaging.dto.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void consumeOrderCancelledEvent(OrderCancelledEvent event) {
        if (event == null || event.getOrderCode() == null || event.getItems() == null || event.getItems().isEmpty()) {
            return;
        }

        SystemInventoryRequest request = SystemInventoryRequest.builder()
                .referenceCode(event.getOrderCode())
                .note(event.getReason())
                .items(event.getItems().stream()
                        .map(it -> InventoryLineRequest.builder()
                                .variantId(it.getVariantId())
                                .quantity(it.getQuantity())
                                .build())
                        .toList())
                .build();

        inventoryService.rollbackInventory(request);
    }
}
