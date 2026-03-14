package com.badminton.shop.service.impl;

import com.badminton.shop.dto.request.LoginRequest;
import com.badminton.shop.dto.request.RegisterRequest;
import com.badminton.shop.dto.response.AuthResponse;
import com.badminton.shop.dto.response.UserResponse;
import com.badminton.shop.entity.User;
import com.badminton.shop.exception.BadRequestException;
import com.badminton.shop.exception.DuplicateResourceException;
import com.badminton.shop.repository.UserRepository;
import com.badminton.shop.security.JwtTokenProvider;
import com.badminton.shop.security.UserDetailsServiceImpl;
import com.badminton.shop.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Đăng ký tài khoản mới với email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' đã được sử dụng");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Đăng ký thành công cho user ID: {}", savedUser.getId());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        return buildAuthResponse(userDetails, savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Đăng nhập với email: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        log.info("Đăng nhập thành công cho user ID: {}", user.getId());
        return buildAuthResponse(userDetails, user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtTokenProvider.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return buildAuthResponse(userDetails, user);
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails, User user) {
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(userResponse)
                .build();
    }
}
