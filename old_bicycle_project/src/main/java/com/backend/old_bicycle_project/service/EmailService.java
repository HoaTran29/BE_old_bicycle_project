package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.PasswordResetToken;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.repository.EmailVerificationRepository;
import com.backend.old_bicycle_project.repository.PasswordResetTokenRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${auth.password-reset-expiration:3600000}")
    private long passwordResetExpiration;

    @Transactional
    public EmailVerification createVerificationToken(User user) {
        emailVerificationRepository.deleteByUser(user);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        return emailVerificationRepository.save(verification);
    }

    @Transactional
    public PasswordResetToken createPasswordResetToken(User user) {
        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(passwordResetExpiration / 1000))
                .build();
        return passwordResetTokenRepository.save(resetToken);
    }

    public Optional<EmailVerification> findByToken(String token) {
        return emailVerificationRepository.findByToken(token);
    }

    public Optional<PasswordResetToken> findPasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token);
    }

    @Transactional
    public void deleteByUser(User user) {
        emailVerificationRepository.deleteByUser(user);
    }

    @Transactional
    public void deletePasswordResetTokensByUser(User user) {
        passwordResetTokenRepository.deleteByUser(user);
    }

    @Async
    public void sendVerificationEmail(User user, String token) {
        sendEmail(
                user.getEmail(),
                "Xac thuc tai khoan - Old Bicycles Marketplace",
                buildVerificationEmailHtml(user, frontendUrl + "/api/auth/verify-email?token=" + token)
        );
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        sendEmail(
                user.getEmail(),
                "Dat lai mat khau - Old Bicycles Marketplace",
                buildPasswordResetEmailHtml(user, frontendUrl + "/reset-password?token=" + token)
        );
    }

    private void sendEmail(String recipient, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Sent email to {}", recipient);
        } catch (Exception e) {
            log.error("Cannot send email to {}: {}", recipient, e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(User user, String verifyUrl) {
        String displayName = user.getFirstName() != null ? user.getFirstName() : user.getEmail();
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; margin:0; padding:20px;">
                  <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:8px; padding:40px; box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                    <h2 style="color:#333;">Xin chao %s!</h2>
                    <p style="color:#666; line-height:1.6;">Vui long bam vao nut ben duoi de xac thuc email cua ban.</p>
                    <div style="text-align:center; margin:30px 0;">
                      <a href="%s" style="background-color:#e67e22; color:#fff; padding:14px 32px; text-decoration:none; border-radius:6px; font-size:16px; font-weight:bold; display:inline-block;">
                        Xac thuc Email
                      </a>
                    </div>
                    <p style="color:#999; font-size:13px;">Link nay co hieu luc trong 24 gio.</p>
                  </div>
                </body>
                </html>
                """.formatted(displayName, verifyUrl);
    }

    private String buildPasswordResetEmailHtml(User user, String resetUrl) {
        String displayName = user.getFirstName() != null ? user.getFirstName() : user.getEmail();
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; margin:0; padding:20px;">
                  <div style="max-width:600px; margin:0 auto; background:#fff; border-radius:8px; padding:40px; box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                    <h2 style="color:#333;">Xin chao %s!</h2>
                    <p style="color:#666; line-height:1.6;">He thong da nhan yeu cau dat lai mat khau cho tai khoan cua ban.</p>
                    <div style="text-align:center; margin:30px 0;">
                      <a href="%s" style="background-color:#e67e22; color:#fff; padding:14px 32px; text-decoration:none; border-radius:6px; font-size:16px; font-weight:bold; display:inline-block;">
                        Dat lai mat khau
                      </a>
                    </div>
                    <p style="color:#999; font-size:13px;">Link nay co hieu luc trong 60 phut. Neu ban khong thuc hien yeu cau nay, hay bo qua email.</p>
                  </div>
                </body>
                </html>
                """.formatted(displayName, resetUrl);
    }
}
