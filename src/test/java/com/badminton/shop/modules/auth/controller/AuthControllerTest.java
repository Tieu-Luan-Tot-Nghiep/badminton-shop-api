package com.badminton.shop.modules.auth.controller;

import com.badminton.shop.modules.auth.dto.AuthResponse;
import com.badminton.shop.modules.auth.dto.UserProfileResponse;
import com.badminton.shop.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_ReturnsWrappedResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .username("user123")
                .role("CUSTOMER")
                .build();
        when(authService.register(any(), anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user123",
                                  "email": "user123@example.com",
                                  "password": "password123",
                                  "phoneNumber": "0123456789"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.username").value("user123"));
    }

    @Test
    void login_ReturnsWrappedResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .username("user123")
                .role("CUSTOMER")
                .build();
        when(authService.login(any(), anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user123",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.token").value("access-token"));
    }

    @Test
    void googleLogin_ReturnsWrappedResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .username("google-user")
                .role("CUSTOMER")
                .build();
        when(authService.loginWithGoogle(any(), anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "firebase-id-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.token").value("access-token"));
    }

    @Test
    void refresh_ReturnsWrappedResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
        when(authService.refreshToken(anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.token").value("new-access-token"));
    }

    @Test
    void logout_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void verifyEmail_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).verifyEmail(anyString(), anyString());

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "verify-token")
                        .param("email", "user123@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void forgotPassword_ReturnsWrappedResponse() throws Exception {
                doNothing().when(authService).forgotPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user123@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void resetPassword_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "reset-token",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void changePassword_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).changePassword(anyString(), any());

        mockMvc.perform(post("/api/auth/change-password")
                        .principal(() -> "user123@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "oldPassword123",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void getCurrentUser_ReturnsWrappedResponse() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .fullName("User 123")
                .email("user123@example.com")
                .avatar("https://cdn.example.com/avatar.png")
                .role("CUSTOMER")
                .build();
        when(authService.getCurrentUserProfile(anyString())).thenReturn(profile);

        mockMvc.perform(get("/api/auth/me")
                        .principal(() -> "user123@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("user123@example.com"));
    }

    @Test
    void updateProfile_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).updateProfile(anyString(), any());

        mockMvc.perform(put("/api/auth/profile")
                        .principal(() -> "user123@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Updated User",
                                  "phoneNumber": "0123456789",
                                  "birthDate": "2000-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void updateAvatar_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).updateAvatar(anyString(), any());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/auth/profile/avatar")
                        .file(file)
                        .principal(() -> "user123@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void resendVerification_ReturnsWrappedResponse() throws Exception {
                doNothing().when(authService).resendVerification(anyString(), anyString());

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user123@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void deleteAccount_ReturnsWrappedResponse() throws Exception {
        doNothing().when(authService).deleteAccount(anyString());

        mockMvc.perform(delete("/api/auth/account")
                        .principal(() -> "user123@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
