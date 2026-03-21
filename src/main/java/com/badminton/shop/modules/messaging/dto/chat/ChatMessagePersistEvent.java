package com.badminton.shop.modules.messaging.dto.chat;

import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.messaging.entity.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePersistEvent implements Serializable {
    private String messageId;
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
}
