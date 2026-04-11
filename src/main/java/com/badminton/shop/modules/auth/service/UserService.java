package com.badminton.shop.modules.auth.service;

import com.badminton.shop.modules.auth.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface UserService {
    Page<UserProfileResponse> adminGetUsers(String keyword, String roleStr, Boolean active, int page, int size);
    UserProfileResponse adminGetUserById(Long id);
    UserProfileResponse adminUpdateUserStatus(Long id, boolean active);
    Map<String, Object> adminGetUserStats();
}
