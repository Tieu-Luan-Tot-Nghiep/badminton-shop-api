package com.badminton.shop.dto.response;

import com.badminton.shop.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
