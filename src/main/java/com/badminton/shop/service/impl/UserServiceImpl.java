package com.badminton.shop.service.impl;

import com.badminton.shop.dto.request.ChangePasswordRequest;
import com.badminton.shop.dto.request.UpdateProfileRequest;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.dto.response.UserResponse;
import com.badminton.shop.entity.User;
import com.badminton.shop.exception.BadRequestException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.UserRepository;
import com.badminton.shop.service.UserService;
import com.badminton.shop.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "email", email));
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Page<User> userPage = userRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return AppUtils.toPageResponse(userPage, userPage.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        User updated = userRepository.save(user);
        log.info("Cập nhật thông tin người dùng ID: {}", userId);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Đổi mật khẩu thành công cho user ID: {}", userId);
    }

    @Override
    @Transactional
    public void disableUser(Long userId) {
        User user = findById(userId);
        user.setEnabled(false);
        userRepository.save(user);
        log.info("Vô hiệu hóa tài khoản user ID: {}", userId);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
