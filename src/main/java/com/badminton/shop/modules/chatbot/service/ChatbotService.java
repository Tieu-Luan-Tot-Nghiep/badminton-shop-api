package com.badminton.shop.modules.chatbot.service;

import com.badminton.shop.modules.chatbot.dto.ChatbotAskResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotCloseSessionResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotSessionStateResponse;

public interface ChatbotService {

    ChatbotAskResponse ask(String principalEmail, String question);

    ChatbotCloseSessionResponse closeSession(String principalEmail);

    ChatbotSessionStateResponse getSessionState(String principalEmail);
}
