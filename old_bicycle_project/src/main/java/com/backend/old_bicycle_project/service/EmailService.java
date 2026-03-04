package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.repository.EmailVerificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public EmailVerification createVerificationToken(User user) {
        // Xóa token cũ nếu có
        emailVerificationRepository.deleteByUser(user);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24)) // Token có giá trị 24h
                .build();
        return emailVerificationRepository.save(verification);
    }

    public Optional<EmailVerification> findByToken(String token) {
        return emailVerificationRepository.findByToken(token);
    }

    @Transactional
    public void deleteByUser(User user) {
        emailVerificationRepository.deleteByUser(user);
    }

    @Async
    public void sendVerificationEmail(User user, String token) {
        try {
            String verifyUrl = frontendUrl + "/api/auth/verify-email?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Xác thực tài khoản - Old Bicycles Marketplace");
            helper.setText(buildVerificationEmailHtml(user, verifyUrl), true);

            mailSender.send(message);
            log.info("Đã gửi email xác thực tới: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác thực tới {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(User user, String verifyUrl) {
        String displayName = (user.getFirstName() != null ? user.getFirstName() : user.getEmail());
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; margin:0; padding:20px;">
                  <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:8px; padding:40px; box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                    <div style="text-align:center; margin-bottom:30px;">
                      <h1 style="color:#e67e22; font-size:28px;">🚲 Old Bicycles Marketplace</h1>
                    </div>
                    <h2 style="color:#333;">Xin chào %s!</h2>
                    <p style="color:#666; line-height:1.6;">
                      Cảm ơn bạn đã đăng ký tài khoản. Vui lòng click vào nút bên dưới để xác thực email của bạn.
                    </p>
                    <div style="text-align:center; margin:30px 0;">
                      <a href="%s"
                         style="background-color:#e67e22; color:#fff; padding:14px 32px; text-decoration:none;
                                border-radius:6px; font-size:16px; font-weight:bold; display:inline-block;">
                        ✅ Xác thực Email
                      </a>
                    </div>
                    <p style="color:#999; font-size:13px;">
                      Link này có hiệu lực trong vòng <strong>24 giờ</strong>. Nếu bạn không đăng ký tài khoản, hãy bỏ qua email này.
                    </p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="color:#aaa; font-size:12px; text-align:center;">© 2024 Old Bicycles Marketplace. All rights reserved.</p>
                  </div>
                </body>
                </html>
                """.formatted(displayName, verifyUrl);
    }
}
