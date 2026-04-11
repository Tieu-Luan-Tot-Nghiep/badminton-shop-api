package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.auth.dto.UserProfileResponse;
import com.badminton.shop.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserProfileResponse> response = userService.adminGetUsers(keyword, role, active, page, size);
        return ResponseEntity.ok(ApiResponse.success("Get users successful", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {
        UserProfileResponse response = userService.adminGetUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Get user successful", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        UserProfileResponse response = userService.adminUpdateUserStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("Update user status successful", response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats() {
        Map<String, Object> stats = userService.adminGetUserStats();
        return ResponseEntity.ok(ApiResponse.success("Get user stats successful", stats));
    }
}
