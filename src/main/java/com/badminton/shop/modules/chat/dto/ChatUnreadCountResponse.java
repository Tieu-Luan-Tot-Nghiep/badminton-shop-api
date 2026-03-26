package com.badminton.shop.modules.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUnreadCountResponse {
    private String roomId;
    private Integer unreadCount;
}
