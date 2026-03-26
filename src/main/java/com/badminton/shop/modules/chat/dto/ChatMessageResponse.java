package com.badminton.shop.modules.chat.dto;

import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.chat.entity.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String id;
    private String roomId;
    private Long senderId;
    private String senderEmail;
    private String senderName;
    private Role senderRole;
    private ChatMessageType messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private LocalDateTime sentAt;
    private LocalDateTime readByCustomerAt;
    private LocalDateTime readByAdminAt;
}
