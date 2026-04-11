package com.badminton.shop.modules.auth.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.dto.UserProfileResponse;
import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.auth.repository.UserSpecification;
import com.badminton.shop.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> adminGetUsers(String keyword, String roleStr, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<User> spec = UserSpecification.filterUsers(keyword, roleStr, active);
        
        return userRepository.findAll(spec, pageable).map(this::toUserProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse adminGetUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id));
        return toUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse adminUpdateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id));
        user.setIsActive(active);
        User saved = userRepository.save(user);
        return toUserProfileResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> adminGetUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.count((root, query, cb) -> cb.equal(root.get("isActive"), true));
        long admins = userRepository.count((root, query, cb) -> cb.equal(root.get("role"), Role.ADMIN));
        long customers = userRepository.count((root, query, cb) -> cb.equal(root.get("role"), Role.CUSTOMER));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("admins", admins);
        stats.put("customers", customers);
        return stats;
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        // Permissions match the logic from AuthServiceImpl
        List<String> permissions = List.of();
        if (Role.ADMIN.equals(user.getRole())) {
            permissions = List.of("manage:products", "manage:orders", "manage:users", "view:reports", "manage:promotions");
        } else if (Role.CUSTOMER.equals(user.getRole())) {
            permissions = List.of("view:products", "create:orders", "view:orders", "manage:profile");
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .permissions(permissions)
                .isEmailVerified(user.getIsEmailVerified() != null ? user.getIsEmailVerified() : false)
                .isActive(user.getIsActive() != null ? user.getIsActive() : false)
                .createdAt(user.getCreatedAt().toString())
                .build();
    }
}
