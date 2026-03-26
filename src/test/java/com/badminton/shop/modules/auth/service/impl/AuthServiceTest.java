package com.badminton.shop.modules.auth.service.impl;

import com.badminton.shop.exception.EmailNotVerifiedException;
import com.badminton.shop.exception.InvalidCredentialsException;
import com.badminton.shop.exception.TooManyRequestsException;
import com.badminton.shop.exception.UserAlreadyExistsException;
import com.badminton.shop.modules.auth.dto.*;
import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.auth.service.RateLimitService;
import com.badminton.shop.modules.messaging.dto.AvatarUpdateMessage;
import com.badminton.shop.modules.messaging.dto.EmailMessage;
import com.badminton.shop.utils.jwt.JwtUtil;
import com.badminton.shop.utils.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private S3Service s3Service;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "forgotPasswordCooldownSeconds", 60);
        ReflectionTestUtils.setField(authService, "forgotPasswordMaxRequests", 3);
        ReflectionTestUtils.setField(authService, "forgotPasswordWindowSeconds", 300);
        ReflectionTestUtils.setField(authService, "resendVerificationCooldownSeconds", 60);
        ReflectionTestUtils.setField(authService, "resendVerificationMaxRequests", 3);
        ReflectionTestUtils.setField(authService, "resendVerificationWindowSeconds", 300);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("hashedPassword")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(true)
                .isDeleted(false)
                .avatar("oldAvatar.jpg")
                .build();

        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .phoneNumber("0123456789")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
    }

    @Test
    void register_Success() {
        when(rateLimitService.isInCooldown(anyString(), anyInt())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.register(registerRequest, "127.0.0.1");

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(EmailMessage.class));
    }

    @Test
    void register_UserAlreadyExists() {
        when(rateLimitService.isInCooldown(anyString(), anyInt())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest, "127.0.0.1"));
    }

    @Test
    void login_Success() {
        when(rateLimitService.isLocked(anyString())).thenReturn(false);
        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refreshToken");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(loginRequest, "127.0.0.1");

        assertNotNull(response);
        assertEquals("accessToken", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_InvalidCredentials() {
        when(rateLimitService.isLocked(anyString())).thenReturn(false);
        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Bad credentials"));
        when(rateLimitService.trackLoginFailure(anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest, "127.0.0.1"));
    }

    @Test
    void login_EmailNotVerified() {
        testUser.setIsEmailVerified(false);
        when(rateLimitService.isLocked(anyString())).thenReturn(false);
        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(loginRequest, "127.0.0.1"));
    }

    @Test
    void verifyEmail_Success() {
        testUser.setIsEmailVerified(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("valid-token");

        authService.verifyEmail("valid-token", "test@example.com");

        assertTrue(testUser.getIsEmailVerified());
        verify(userRepository).save(testUser);
    }

    @Test
    void resendVerification_Success() {
        testUser.setIsEmailVerified(false);
        when(rateLimitService.isInCooldown(anyString(), anyInt())).thenReturn(false);
        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.resendVerification("test@example.com", "127.0.0.1");

        verify(valueOperations).set(startsWith("verify:"), anyString(), anyLong(), any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(EmailMessage.class));
    }

    @Test
    void forgotPassword_Success() {
        when(rateLimitService.isInCooldown(anyString(), anyInt())).thenReturn(false);
        when(rateLimitService.isRateLimited(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.forgotPassword("test@example.com", "127.0.0.1");

        verify(valueOperations).set(startsWith("reset:"), anyString(), anyLong(), any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(EmailMessage.class));
    }

    @Test
    void resetPassword_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("test@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        authService.resetPassword("valid-token", "newPassword123");

        assertEquals("newHashedPassword", testUser.getPasswordHash());
        verify(userRepository).save(testUser);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword123");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        authService.changePassword("test@example.com", request);

        assertEquals("newHashedPassword", testUser.getPasswordHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setPhoneNumber("0987654321");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        authService.updateProfile("test@example.com", request);

        assertEquals("Updated Name", testUser.getFullName());
        assertEquals("0987654321", testUser.getPhoneNumber());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateAvatar_Success() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(s3Service.uploadFile(anyString(), anyString(), any(MultipartFile.class))).thenReturn("http://s3.url/avatar.jpg");

        authService.updateAvatar("test@example.com", file);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AvatarUpdateMessage.class));
    }

    @Test
    void deleteAccount_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        authService.deleteAccount("test@example.com");

        verify(userRepository).delete(testUser);
    }
}
