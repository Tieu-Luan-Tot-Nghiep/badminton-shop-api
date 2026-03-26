package com.badminton.shop.modules.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotAskRequest {

    @NotBlank(message = "question must not be blank")
    private String question;
}
