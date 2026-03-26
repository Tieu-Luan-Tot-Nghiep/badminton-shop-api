package com.badminton.shop.modules.chat.entity;

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
@Document(collection = "chat_rooms")
public class ChatRoomDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long customerId;

    private String customerEmail;

    private String customerName;

    private String lastMessagePreview;

    @Indexed
    private LocalDateTime lastMessageAt;

    private Integer adminUnreadCount;

    private Integer customerUnreadCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
