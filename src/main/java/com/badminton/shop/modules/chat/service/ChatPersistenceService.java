package com.badminton.shop.modules.chat.service;

import com.badminton.shop.modules.chat.dto.ChatMessagePersistEvent;

public interface ChatPersistenceService {

    void persistMessage(ChatMessagePersistEvent event);
}
