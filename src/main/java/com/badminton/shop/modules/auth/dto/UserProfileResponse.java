package com.badminton.shop.modules.auth.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private LocalDate birthDate;
    private String avatar;
    private String role;
    private List<String> permissions;
    private String username;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private String createdAt;
}
