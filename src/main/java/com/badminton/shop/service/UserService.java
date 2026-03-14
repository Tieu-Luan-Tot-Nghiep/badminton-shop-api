package com.badminton.shop.service;

import com.badminton.shop.dto.request.ChangePasswordRequest;
import com.badminton.shop.dto.request.UpdateProfileRequest;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.dto.response.UserResponse;

public interface UserService {

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    PageResponse<UserResponse> getAllUsers(int page, int size);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void disableUser(Long userId);
}
