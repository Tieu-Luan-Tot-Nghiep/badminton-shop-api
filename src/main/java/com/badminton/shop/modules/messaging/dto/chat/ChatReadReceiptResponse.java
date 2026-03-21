package com.badminton.shop.modules.messaging.dto.chat;

import com.badminton.shop.modules.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadReceiptResponse {
    private String roomId;
    private Role readerRole;
    private LocalDateTime readAt;
}
