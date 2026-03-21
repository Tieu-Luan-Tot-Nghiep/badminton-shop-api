package com.badminton.shop.utils.email;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendForgotPasswordEmail(String to, String token);
    void sendOrderCancellationEmail(String to, String orderCode, String reason);
}
