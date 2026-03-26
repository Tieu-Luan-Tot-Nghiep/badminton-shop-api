package com.badminton.shop.modules.chatbot.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ChatbotCloseSessionResponse {
    boolean persisted;
    String summary;
    List<String> recommendedProducts;
    LocalDateTime persistedAt;
}
