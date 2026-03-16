package com.badminton.shop.modules.auth.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.exception.EmailNotVerifiedException;
import com.badminton.shop.exception.InvalidCredentialsException;
import com.badminton.shop.exception.UserAlreadyExistsException;
import com.badminton.shop.exception.TooManyRequestsException;
import com.badminton.shop.modules.auth.dto.*;
import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.auth.service.AuthService;
import com.badminton.shop.modules.auth.service.RateLimitService;
import com.badminton.shop.modules.messaging.dto.AvatarUpdateMessage;
import com.badminton.shop.modules.messaging.dto.EmailMessage;
import com.badminton.shop.utils.jwt.JwtUtil;
import com.badminton.shop.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final RateLimitService rateLimitService;
    private final S3Service s3Service;

    private static final String REDIS_VERIFY_PREFIX = "verify:";
    private static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String REDIS_RESET_PASSWORD_PREFIX = "reset:";

    @Override
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        // 1. Kiểm tra Cooldown 30s giữa các request từ cùng IP/Email
        if (rateLimitService.isInCooldown(ipAddress, 30) || rateLimitService.isInCooldown(request.getEmail(), 30)) {
            throw new TooManyRequestsException("Vui lòng đợi 30 giây trước khi thử lại.");
        }

        // 2. Giới hạn tối đa 5 request/phút trên mỗi IP
        if (rateLimitService.isRateLimited(ipAddress, 5, 60)) {
            throw new TooManyRequestsException("Bạn đã gửi quá nhiều yêu cầu đăng ký. Vui lòng thử lại sau 1 phút.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use");
        }

        String verificationToken = UUID.randomUUID().toString();

        // Lưu token vào Redis với TTL là 10 phút
        redisTemplate.opsForValue().set(REDIS_VERIFY_PREFIX + request.getEmail(), verificationToken, 10,
                TimeUnit.MINUTES);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        userRepository.save(user);

        // Gửi message vào RabbitMQ
        EmailMessage message = EmailMessage.builder()
                .email(user.getEmail())
                .token(verificationToken)
                .type(EmailMessage.EmailType.VERIFICATION)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_VERIFICATION_ROUTING_KEY,
                message);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRole().name())
                .build();

        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Lưu Refresh Token vào Redis
        redisTemplate.opsForValue().set(REDIS_REFRESH_TOKEN_PREFIX + user.getEmail(), refreshToken, 7,
                TimeUnit.DAYS);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request, String ipAddress) {
        // 1. Kiểm tra xem IP hoặc Username có đang bị khóa không
        if (rateLimitService.isLocked(ipAddress)) {
            throw new TooManyRequestsException(
                    "IP của bạn đã bị khóa do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }
        if (rateLimitService.isLocked(request.getUsername())) {
            throw new TooManyRequestsException(
                    "Tài khoản này đã bị khóa do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        // 2. Giới hạn 5 request/phút cho login
        if (rateLimitService.isRateLimited(ipAddress, 5, 60)) {
            throw new TooManyRequestsException("Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau 1 phút.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // Đăng nhập thành công -> Reset bộ đếm lỗi
            rateLimitService.resetLoginFailures(request.getUsername());
            rateLimitService.resetLoginFailures(ipAddress);

        } catch (BadCredentialsException e) {
            // Đăng nhập thất bại -> Theo dõi và khóa nếu cần
            boolean isLockedNow = rateLimitService.trackLoginFailure(request.getUsername()) ||
                    rateLimitService.trackLoginFailure(ipAddress);

            if (isLockedNow) {
                throw new TooManyRequestsException("Bạn đã nhập sai quá 5 lần. Tài khoản/IP đã bị khóa trong 15 phút.");
            }
            throw new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác."));

        if (!user.getIsEmailVerified()) {
            throw new EmailNotVerifiedException("Vui lòng xác thực email của bạn trước khi đăng nhập");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Lưu Refresh Token vào Redis
        redisTemplate.opsForValue().set(REDIS_REFRESH_TOKEN_PREFIX + user.getEmail(), refreshToken, 7,
                TimeUnit.DAYS);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtUtil.validateToken(refreshToken, userDetails)) {
            String savedRefreshToken = (String) redisTemplate.opsForValue().get(REDIS_REFRESH_TOKEN_PREFIX + username);

            if (refreshToken.equals(savedRefreshToken)) {
                String newToken = jwtUtil.generateToken(userDetails);
                String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

                // Rotate Refresh Token
                redisTemplate.opsForValue().set(REDIS_REFRESH_TOKEN_PREFIX + username, newRefreshToken, 7,
                        TimeUnit.DAYS);

                return AuthResponse.builder()
                        .token(newToken)
                        .refreshToken(newRefreshToken)
                        .username(username)
                        .role(userDetails.getAuthorities().iterator().next().getAuthority())
                        .build();
            }
        }
        throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
    }

    @Override
    public void logout(String refreshToken) {
        String username = jwtUtil.extractUsername(refreshToken);
        redisTemplate.delete(REDIS_REFRESH_TOKEN_PREFIX + username);
    }

    @Override
    public void verifyEmail(String token, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này"));

        if (user.getIsEmailVerified()) {
            throw new RuntimeException("Email này đã được verify, bạn hãy đăng nhập.");
        }

        String savedToken = (String) redisTemplate.opsForValue().get(REDIS_VERIFY_PREFIX + email);

        if (savedToken == null) {
            throw new RuntimeException("Token xác thực đã hết hạn hoặc không tồn tại. Vui lòng đăng ký lại.");
        }

        if (!savedToken.equals(token)) {
            throw new RuntimeException("Token xác thực không hợp lệ");
        }

        user.setIsEmailVerified(true);
        userRepository.save(user);

        // Xóa token khỏi Redis sau khi verify thành công
        redisTemplate.delete(REDIS_VERIFY_PREFIX + email);
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này"));

        String resetToken = UUID.randomUUID().toString();

        // Lưu token vào Redis với TTL là 15 phút
        redisTemplate.opsForValue().set(REDIS_RESET_PASSWORD_PREFIX + resetToken, email, 15, TimeUnit.MINUTES);

        // Gửi message vào RabbitMQ
        EmailMessage message = EmailMessage.builder()
                .email(email)
                .token(resetToken)
                .type(EmailMessage.EmailType.FORGOT_PASSWORD)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_VERIFICATION_ROUTING_KEY,
                message);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String email = (String) redisTemplate.opsForValue().get(REDIS_RESET_PASSWORD_PREFIX + token);
        
        if (email == null) {
            throw new RuntimeException("Token đặt lại mật khẩu đã hết hạn hoặc không tồn tại.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Xóa token khỏi Redis sau khi reset thành công
        redisTemplate.delete(REDIS_RESET_PASSWORD_PREFIX + token);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        userRepository.save(user);
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void updateAvatar(String email, MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Kích thước ảnh không được vượt quá 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Chỉ cho phép tải lên tệp tin hình ảnh");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        String oldAvatarUrl = user.getAvatar();
        String avatarUrl = s3Service.uploadFile("avatar", user.getId().toString(), file);
        
        // Gửi message cập nhật avatar qua RabbitMQ
        AvatarUpdateMessage message = AvatarUpdateMessage.builder()
                .email(email)
                .avatarUrl(avatarUrl)
                .oldAvatarUrl(oldAvatarUrl)
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.AVATAR_UPDATE_ROUTING_KEY,
                message
        );
    }

    @Override
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getIsEmailVerified()) {
            throw new RuntimeException("Email của bạn đã được xác thực trước đó.");
        }

        String verificationToken = UUID.randomUUID().toString();

        // Lưu token vào Redis với TTL là 10 phút
        redisTemplate.opsForValue().set(REDIS_VERIFY_PREFIX + email, verificationToken, 10,
                TimeUnit.MINUTES);

        // Gửi message vào RabbitMQ
        EmailMessage message = EmailMessage.builder()
                .email(email)
                .token(verificationToken)
                .type(EmailMessage.EmailType.VERIFICATION)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_VERIFICATION_ROUTING_KEY,
                message);
    }

    @Override
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        userRepository.delete(user);
    }
}
