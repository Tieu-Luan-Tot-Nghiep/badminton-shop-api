package com.badminton.shop.modules.inventory.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.inventory.dto.InventoryLineRequest;
import com.badminton.shop.modules.inventory.dto.SystemInventoryRequest;
import com.badminton.shop.modules.inventory.service.InventoryService;
import com.badminton.shop.modules.messaging.dto.InventoryCommandMessage;
import com.badminton.shop.modules.messaging.dto.InventoryCommandType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandConsumer {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_COMMAND_QUEUE)
    public void consumeInventoryCommand(InventoryCommandMessage message) {
        if (message == null || message.getCommandType() == null || message.getItems() == null || message.getItems().isEmpty()) {
            return;
        }

        SystemInventoryRequest request = SystemInventoryRequest.builder()
                .referenceCode(message.getReferenceCode())
                .note(message.getNote())
                .items(message.getItems().stream()
                        .map(it -> InventoryLineRequest.builder()
                                .variantId(it.getVariantId())
                                .quantity(it.getQuantity())
                                .build())
                        .toList())
                .build();

        if (message.getCommandType() == InventoryCommandType.RESERVE) {
            inventoryService.reserveInventory(request);
            return;
        }
        if (message.getCommandType() == InventoryCommandType.COMMIT) {
            inventoryService.commitInventory(request);
            return;
        }
        if (message.getCommandType() == InventoryCommandType.ROLLBACK) {
            inventoryService.rollbackInventory(request);
            return;
        }

        log.warn("Unsupported inventory command: {}", message.getCommandType());
    }
}
