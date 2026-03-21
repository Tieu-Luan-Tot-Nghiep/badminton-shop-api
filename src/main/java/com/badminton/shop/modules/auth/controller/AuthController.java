package com.badminton.shop.modules.auth.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.auth.dto.*;
import com.badminton.shop.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        AuthResponse response = authService.register(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Register successful.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        AuthResponse response = authService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logout successful.", null));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@RequestParam String token, @RequestParam String email) {
        authService.verifyEmail(token, email);
        return ResponseEntity.ok(ApiResponse.success("Xac thuc email thanh cong! Ban hien co the dang nhap.", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Da gui link dat lai mat khau vao email cua ban. Vui long kiem tra hop thu.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Dat lai mat khau thanh cong! Vui long quay lai ung dung de dang nhap.", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Doi mat khau thanh cong.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Principal principal) {
        UserProfileResponse response = authService.getCurrentUserProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Get profile successful.", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {
        authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat thong tin ca nhan thanh cong.", null));
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<Object>> updateAvatar(Principal principal, @RequestParam("file") MultipartFile file) {
        authService.updateAvatar(principal.getName(), file);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat anh dai dien thanh cong.", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.success("Da gui ma xac thuc moi. Vui long kiem tra email cua ban.", null));
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Object>> deleteAccount(Principal principal) {
        authService.deleteAccount(principal.getName());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Tai khoan cua ban da duoc vo hieu hoa.", null));
    }
}
