package com.badminton.shop.modules.membership.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.membership.dto.response.PointHistoryResponse;
import com.badminton.shop.modules.membership.dto.response.UserMembershipResponse;
import com.badminton.shop.modules.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserMembershipResponse>> getMyMembership(Principal principal) {
        
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserMembershipResponse response = membershipService.getUserMembership(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hạng thành viên thành công", response));
    }

    @GetMapping("/me/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PointHistoryResponse>>> getMyPointHistory(Principal principal) {
        
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<PointHistoryResponse> response = membershipService.getUserPointHistory(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử tích điểm thành công", response));
    }
}
