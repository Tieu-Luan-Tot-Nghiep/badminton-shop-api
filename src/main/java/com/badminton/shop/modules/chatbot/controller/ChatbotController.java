package com.badminton.shop.modules.chatbot.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotAskRequest;
import com.badminton.shop.modules.chatbot.dto.ChatbotAskResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotCloseSessionResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotSessionStateResponse;
import com.badminton.shop.modules.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<ChatbotAskResponse>> ask(
            Principal principal,
            @Valid @RequestBody ChatbotAskRequest request
    ) {
        if (isUnauthenticated(principal)) {
            return unauthorizedResponse("Vui lòng đăng nhập để sử dụng chatbot.");
        }

        ChatbotAskResponse response = chatbotService.ask(principal.getName(), request.getQuestion());
        return ResponseEntity.ok(ApiResponse.success("Chatbot answered successfully.", response));
    }

    @PostMapping("/close-session")
    public ResponseEntity<ApiResponse<ChatbotCloseSessionResponse>> closeSession(Principal principal) {
        if (isUnauthenticated(principal)) {
            return unauthorizedResponse("Vui lòng đăng nhập để đóng phiên chatbot.");
        }

        ChatbotCloseSessionResponse response = chatbotService.closeSession(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Chatbot session closed.", response));
    }

    @GetMapping("/session-state")
    public ResponseEntity<ApiResponse<ChatbotSessionStateResponse>> sessionState(Principal principal) {
        if (isUnauthenticated(principal)) {
            return unauthorizedResponse("Vui lòng đăng nhập để xem trạng thái phiên chatbot.");
        }

        ChatbotSessionStateResponse response = chatbotService.getSessionState(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Chatbot session state fetched.", response));
    }

    private boolean isUnauthenticated(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return true;
        }
        return "anonymousUser".equalsIgnoreCase(principal.getName().trim());
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorizedResponse(String message) {
        ApiResponse<T> body = new ApiResponse<>(message, "error", HttpStatus.UNAUTHORIZED.value(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
