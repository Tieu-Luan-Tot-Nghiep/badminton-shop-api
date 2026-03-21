package com.badminton.shop.modules.messaging.entity;

import com.badminton.shop.modules.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessageDocument {

    @Id
    private String id;

    @Indexed
    private String roomId;

    @Indexed
    private Long senderId;

    private String senderEmail;

    private String senderName;

    private Role senderRole;

    private ChatMessageType messageType;

    private String content;

    private String fileUrl;

    private String fileName;

    @Indexed
    private LocalDateTime sentAt;

    private LocalDateTime readByCustomerAt;

    private LocalDateTime readByAdminAt;
}
