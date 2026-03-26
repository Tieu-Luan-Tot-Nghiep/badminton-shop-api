package com.badminton.shop.modules.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private String roomId;
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private Integer adminUnreadCount;
    private Integer customerUnreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
