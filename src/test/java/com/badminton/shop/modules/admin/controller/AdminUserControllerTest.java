package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.modules.auth.dto.AdminCreateUserRequest;
import com.badminton.shop.modules.auth.dto.UserProfileResponse;
import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminUserController adminUserController = new AdminUserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();
    }

    @Test
    void createUser_ReturnsCreatedWrappedResponse() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(10L)
                .username("staff01")
                .email("staff01@example.com")
                .fullName("Staff One")
                .role(Role.CUSTOMER.name())
                .isActive(true)
                .isEmailVerified(true)
                .build();
        when(userService.adminCreateUser(any(AdminCreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "staff01",
                                  "email": "staff01@example.com",
                                  "password": "password123",
                                  "fullName": "Staff One",
                                  "phoneNumber": "0123456789",
                                  "role": "CUSTOMER",
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.username").value("staff01"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }
}