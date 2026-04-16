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
                                .requestMatchers("/api/inventory/system/**").hasRole("ADMIN")
                                .requestMatchers("/api/inventory/admin/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/reviews/my").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/search/products/**", "/api/products/search/**","/api/search/products/image**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/orders/vnpay-return", "/api/orders/vnpay-ipn").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/reviews/*", "/api/reviews/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/featured", "/api/products/new").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cart/**", "/api/cart").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/brands/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/admin", "/api/products/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").permitAll()
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
