package com.badminton.shop.modules.chatbot.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatbotProductSuggestion {
    Long id;
    String name;
    String slug;
    Double basePrice;
    String brandName;
    String shortDescription;
}
