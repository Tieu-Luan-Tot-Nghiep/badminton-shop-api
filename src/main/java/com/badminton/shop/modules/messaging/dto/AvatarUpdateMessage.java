package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUpdateMessage implements Serializable {
    private String email;
    private String avatarUrl;
    private String oldAvatarUrl;
}
