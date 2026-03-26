package com.badminton.shop.modules.auth.service;

import com.badminton.shop.modules.auth.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    AuthResponse register(RegisterRequest request, String ipAddress);
    AuthResponse login(LoginRequest request, String ipAddress);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
    void verifyEmail(String token, String email);
    void forgotPassword(String email, String ipAddress);
    void resetPassword(String token, String newPassword);
    void changePassword(String email, ChangePasswordRequest request);
    void updateProfile(String email, UpdateProfileRequest request);
    UserProfileResponse getCurrentUserProfile(String email);

    void updateAvatar(String email, MultipartFile file);
    
    void resendVerification(String email, String ipAddress);
    void deleteAccount(String email);
}
