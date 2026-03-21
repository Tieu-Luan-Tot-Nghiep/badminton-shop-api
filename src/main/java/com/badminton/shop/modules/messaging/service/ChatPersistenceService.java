package com.badminton.shop.modules.messaging.service;

import com.badminton.shop.modules.messaging.dto.chat.ChatMessagePersistEvent;

public interface ChatPersistenceService {

    void persistMessage(ChatMessagePersistEvent event);
}
