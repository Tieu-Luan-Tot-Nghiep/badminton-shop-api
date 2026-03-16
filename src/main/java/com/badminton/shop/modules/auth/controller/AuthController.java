package com.badminton.shop.modules.auth.controller;

import com.badminton.shop.modules.auth.dto.*;
import com.badminton.shop.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.register(request, ipAddress));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, ipAddress));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token, @RequestParam String email) {
        authService.verifyEmail(token, email);
        return ResponseEntity.ok("Xác thực email thành công! Bạn hiện có thể đăng nhập.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("Đã gửi link đặt lại mật khẩu vào email của bạn. Vui lòng kiểm tra hộp thư.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Đặt lại mật khẩu thành công! Vui lòng quay lại ứng dụng để đăng nhập.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok("Đổi mật khẩu thành công.");
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(authService.getCurrentUserProfile(principal.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {
        authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok("Cập nhật thông tin cá nhân thành công.");
    }

    @PostMapping("/profile/avatar")
    public ResponseEntity<String> updateAvatar(Principal principal, @RequestParam("file") MultipartFile file) {
        authService.updateAvatar(principal.getName(), file);
        return ResponseEntity.ok("Cập nhật ảnh đại diện thành công.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authService.resendVerification(email);
        return ResponseEntity.ok("Đã gửi mã xác thực mới. Vui lòng kiểm tra email của bạn.");
    }

    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(Principal principal) {
        authService.deleteAccount(principal.getName());
        return ResponseEntity.ok("Tài khoản của bạn đã được vô hiệu hóa.");
    }
}
