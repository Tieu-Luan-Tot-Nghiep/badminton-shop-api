package com.badminton.shop.modules.membership.controller;

import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.membership.dto.response.MembershipTierResponse;
import com.badminton.shop.modules.membership.dto.response.PointHistoryResponse;
import com.badminton.shop.modules.membership.dto.response.UserMembershipResponse;
import com.badminton.shop.modules.membership.service.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.security.Principal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MembershipController.class)
@AutoConfigureMockMvc(addFilters = false)
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipService membershipService;

    @MockitoBean
    private com.badminton.shop.utils.jwt.JwtUtil jwtUtil;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getMyMembership_ShouldReturnMembershipData() throws Exception {
        MembershipTierResponse tier = new MembershipTierResponse(1L, "BRONZE", 0, BigDecimal.ZERO, "Benefits");
        UserMembershipResponse response = new UserMembershipResponse(1L, "user@example.com", tier, 150, 150, 50, "SILVER");

        when(membershipService.getUserMembership(1L)).thenReturn(response);

        mockMvc.perform(get("/api/memberships/me")
            .principal((Principal) () -> "user@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.currentPoints").value(150))
                .andExpect(jsonPath("$.data.tier.name").value("BRONZE"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getMyPointHistory_ShouldReturnHistoryList() throws Exception {
        PointHistoryResponse history = new PointHistoryResponse(1L, 100, "EARNED", 12L, LocalDateTime.now());
        when(membershipService.getUserPointHistory(1L)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/memberships/me/history")
                .principal((Principal) () -> "user@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].points").value(100));
    }
}
