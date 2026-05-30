package com.badminton.shop.config;

import com.badminton.shop.modules.auth.security.oauth2.CustomOAuth2UserService;
import com.badminton.shop.modules.auth.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.badminton.shop.security.JwtAuthenticationEntryPoint;
import com.badminton.shop.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                            "/api/auth/login",
                            "/api/auth/google/login",
                            "/api/auth/register",
                            "/api/auth/refresh",
                            "/api/auth/verify-email",
                            "/api/auth/forgot-password",
                            "/api/auth/reset-password",
                            "/api/auth/resend-verification",
                            "/api/shipping/webhook/ghn",
                            "/oauth2/**",
                            "/auth/**",
                            "/reset-password.html",
                            "/chat-test.html",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/v3/api-docs",
                            "/v3/api-docs.yaml",
                            "/webjars/**"
                        ).permitAll()
                        // Inventory: system APIs chỉ dùng nội bộ, admin APIs yêu cầu ADMIN
                        .requestMatchers("/api/inventory/system/**").hasRole("ADMIN")
                        .requestMatchers("/api/inventory/admin/**").hasRole("ADMIN")
                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/products/admin", "/api/products/admin/**").hasRole("ADMIN")
                        // Authenticated-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/reviews/my").authenticated()
                        // VNPay callbacks — public (gọi từ cổng thanh toán)
                        .requestMatchers(HttpMethod.GET, "/api/orders/vnpay-return", "/api/orders/vnpay-ipn").permitAll()
                        // Products — public
                        .requestMatchers(HttpMethod.GET,
                            "/api/products",
                            "/api/products/*",
                            "/api/products/categories/**",
                            "/api/products/featured",
                            "/api/products/new",
                            "/api/products/compare",
                            "/api/products/search/existsBySlug",
                            "/api/products/*/recommendations"
                        ).permitAll()
                        // Search — public
                        .requestMatchers(HttpMethod.GET,
                            "/api/search/products",
                            "/api/search/products/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/search/products/by-image").permitAll()
                        // Categories & Brands — public
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/brands/**").permitAll()
                        // Reviews — public (đọc): by id, by product, summary; riêng /my cần auth
                        .requestMatchers(HttpMethod.GET,
                            "/api/reviews/*",
                            "/api/reviews/products/**"
                        ).permitAll()
                        // Promotions — list active và xem theo code là public; validate cũng public
                        .requestMatchers(HttpMethod.GET, "/api/promotions", "/api/promotions/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/promotions/validate").permitAll()
                        // Shipping — provinces/districts/wards public (cần khi điền địa chỉ)
                        // NOTE: /api/shipping/orders/{code} KHÔNG public (thông tin đơn hàng nhạy cảm)
                        .requestMatchers(HttpMethod.GET,
                            "/api/shipping/provinces",
                            "/api/shipping/provinces/*/districts",
                            "/api/shipping/districts/*/wards"
                        ).permitAll()
                        // WebSocket chat
                        .requestMatchers("/ws-chat/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler(oauth2AuthenticationSuccessHandler)
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
