package com.badminton.shop.utils.email.impl;

import com.badminton.shop.utils.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = appUrl + "/api/auth/verify-email?token=" + token + "&email=" + to;
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Xác thực tài khoản Badminton Shop");
            
            String content = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #f9f9f9;\">"
                    + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                    + "<h1 style=\"color: #2c3e50; margin: 0;\">Badminton Shop</h1>"
                    + "<p style=\"color: #7f8c8d; font-size: 14px; margin-top: 5px;\">Nâng tầm cuộc chơi của bạn</p>"
                    + "</div>"
                    + "<hr style=\"border: 0; border-top: 1px solid #e0e0e0; margin: 20px 0;\">"
                    + "<p style=\"font-size: 16px; color: #34495e;\">Chào bạn!</p>"
                    + "<p style=\"font-size: 16px; line-height: 1.6; color: #34495e;\">"
                    + "Cảm ơn bạn đã lựa chọn <b>Badminton Shop</b>. Để hoàn tất việc đăng ký và bảo mật tài khoản, vui lòng nhấn vào nút xác thực bên dưới:"
                    + "</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<a href=\"" + verificationUrl + "\" style=\"display: inline-block; padding: 15px 30px; font-size: 16px; font-weight: bold; color: #ffffff; background-color: #3498db; border-radius: 5px; text-decoration: none; box-shadow: 0 4px 6px rgba(52, 152, 219, 0.3);\">Xác Thực Ngay</a>"
                    + "</div>"
                    + "<div style=\"background-color: #fff3cd; border-left: 5px solid #ffecb5; padding: 15px; margin-bottom: 20px;\">"
                    + "<p style=\"margin: 0; color: #856404; font-size: 14px;\">"
                    + "<b>Lưu ý:</b> Đường link này chỉ có hiệu lực trong vòng <b>2 phút</b>. Sau thời gian này, bạn sẽ cần thực hiện đăng ký lại nếu chưa xác thực."
                    + "</p>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #7f8c8d;\">Nếu không phải bạn đăng ký tài khoản này, vui lòng bỏ qua email này.</p>"
                    + "<hr style=\"border: 0; border-top: 1px solid #e0e0e0; margin: 20px 0;\">"
                    + "<div style=\"text-align: center; color: #95a5a6; font-size: 12px;\">"
                    + "<p>&copy; 2026 Badminton Shop. All rights reserved.</p>"
                    + "</div>"
                    + "</div>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendForgotPasswordEmail(String to, String token) {
        String resetUrl = appUrl + "/auth/reset-password?token=" + token;
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu - Badminton Shop");
            
            String content = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px; background-color: #ffffff;\">"
                    + "<div style=\"text-align: center; margin-bottom: 30px;\">"
                    + "<h1 style=\"color: #2c3e50; margin: 0; font-size: 28px;\">Badminton Shop</h1>"
                    + "<div style=\"width: 50px; height: 3px; background-color: #e74c3c; margin: 10px auto;\"></div>"
                    + "</div>"
                    + "<p style=\"font-size: 16px; color: #34495e;\">Chào bạn,</p>"
                    + "<p style=\"font-size: 16px; line-height: 1.6; color: #34495e;\">"
                    + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn tại <b>Badminton Shop</b>. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này."
                    + "</p>"
                    + "<div style=\"text-align: center; margin: 40px 0;\">"
                    + "<a href=\"" + resetUrl + "\" style=\"display: inline-block; padding: 16px 36px; font-size: 16px; font-weight: bold; color: #ffffff; background-color: #e74c3c; border-radius: 30px; text-decoration: none; box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);\">Đặt Lại Mật Khẩu</a>"
                    + "</div>"
                    + "<div style=\"background-color: #f8f9fa; border-left: 4px solid #dee2e6; padding: 20px; margin-bottom: 30px; border-radius: 4px;\">"
                    + "<p style=\"margin: 0; color: #6c757d; font-size: 14px; line-height: 1.5;\">"
                    + "<b>Ghi chú bảo mật:</b> Link này sẽ hết hạn sau <b>15 phút</b>. Vì lý do bảo mật, bạn không nên chia sẻ email này với bất kỳ ai."
                    + "</p>"
                    + "</div>"
                    + "<hr style=\"border: 0; border-top: 1px solid #eeeeee; margin: 30px 0;\">"
                    + "<div style=\"text-align: center; color: #bdc3c7; font-size: 12px;\">"
                    + "<p>&copy; 2026 Badminton Shop. All rights reserved.</p>"
                    + "</div>"
                    + "</div>";
            
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send forgot password email", e);
        }
    }

    @Override
    public void sendOrderCancellationEmail(String to, String orderCode, String reason) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Đơn hàng đã được hủy - Badminton Shop");

            String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto;\">"
                    + "<h2 style=\"color:#2c3e50\">Thông báo hủy đơn hàng</h2>"
                    + "<p>Đơn hàng <b>" + orderCode + "</b> của bạn đã được hủy thành công.</p>"
                    + "<p><b>Lý do:</b> " + (reason == null ? "Không có" : reason) + "</p>"
                    + "<p>Nếu bạn đã thanh toán trước, bộ phận CSKH/Kế toán sẽ liên hệ để xử lý hoàn tiền theo chính sách.</p>"
                    + "<p>Cảm ơn bạn đã mua sắm tại Badminton Shop.</p>"
                    + "</div>";

            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send order cancellation email", e);
        }
    }
}
