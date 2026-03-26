package com.badminton.shop.modules.chatbot.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ChatbotAskResponse {
    String answer;
    boolean recoveredFromMemory;
    String recoveredMemorySnippet;
    List<ChatbotProductSuggestion> productSuggestions;
    int sessionTurnCount;
    LocalDateTime sessionUpdatedAt;
}
