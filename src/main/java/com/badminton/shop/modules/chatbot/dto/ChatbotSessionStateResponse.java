package com.badminton.shop.modules.chatbot.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ChatbotSessionStateResponse {
    boolean active;
    int turnCount;
    LocalDateTime startedAt;
    LocalDateTime updatedAt;
    List<String> recommendedProducts;
}
