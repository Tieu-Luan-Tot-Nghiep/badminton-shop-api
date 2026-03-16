package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailMessage implements Serializable {
    public enum EmailType {
        VERIFICATION,
        FORGOT_PASSWORD
    }

    private String email;
    private String token;
    private EmailType type;
}
