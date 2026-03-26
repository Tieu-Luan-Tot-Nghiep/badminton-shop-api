package com.badminton.shop.modules.chat.dto;

import com.badminton.shop.modules.chat.entity.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendMessageRequest {

    @NotBlank(message = "roomId is required")
    private String roomId;

    @NotNull(message = "messageType is required")
    private ChatMessageType messageType;

    private String content;

    private String fileUrl;

    private String fileName;
}
