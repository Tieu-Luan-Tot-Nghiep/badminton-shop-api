package com.badminton.shop.service;

import com.badminton.shop.dto.request.LoginRequest;
import com.badminton.shop.dto.request.RegisterRequest;
import com.badminton.shop.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);
}
