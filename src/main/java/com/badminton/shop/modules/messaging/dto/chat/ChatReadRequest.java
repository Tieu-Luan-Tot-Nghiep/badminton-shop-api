package com.badminton.shop.modules.messaging.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadRequest {

    @NotBlank(message = "roomId is required")
    private String roomId;
}
