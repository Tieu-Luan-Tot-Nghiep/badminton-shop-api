package com.badminton.shop.modules.chat.controller;

import com.badminton.shop.modules.chat.dto.ChatMessageResponse;
import com.badminton.shop.modules.chat.dto.ChatReadReceiptResponse;
import com.badminton.shop.modules.chat.dto.ChatReadRequest;
import com.badminton.shop.modules.chat.dto.ChatSendMessageRequest;
import com.badminton.shop.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid ChatSendMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.sendMessage(principal.getName(), request);
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/chat.sent",
                response
        );
    }

    @MessageMapping("/chat.read")
    public void markRead(@Valid ChatReadRequest request, Principal principal) {
        ChatReadReceiptResponse response = chatService.markRoomAsRead(principal.getName(), request.getRoomId());
        messagingTemplate.convertAndSend("/topic/chat.room." + request.getRoomId() + ".read", response);
    }
}
