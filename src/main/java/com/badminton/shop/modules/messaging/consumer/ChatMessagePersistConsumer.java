package com.badminton.shop.modules.messaging.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.chat.ChatMessagePersistEvent;
import com.badminton.shop.modules.messaging.service.ChatPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessagePersistConsumer {

    private final ChatPersistenceService chatPersistenceService;

    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_PERSIST_QUEUE)
    public void consume(ChatMessagePersistEvent event) {
        if (event == null || event.getRoomId() == null || event.getSenderId() == null) {
            log.warn("[chat] Invalid chat persist event payload");
            return;
        }

        chatPersistenceService.persistMessage(event);
    }
}
