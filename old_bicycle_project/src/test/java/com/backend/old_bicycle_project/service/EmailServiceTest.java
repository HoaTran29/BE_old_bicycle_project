package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.entity.EmailVerification;
import com.backend.old_bicycle_project.entity.PasswordResetToken;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.repository.EmailVerificationRepository;
import com.backend.old_bicycle_project.repository.PasswordResetTokenRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendVerificationEmailUsesExternalTemplateWithVietnameseContent() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://public.example.com");
        ReflectionTestUtils.setField(emailService, "fromEmail", "mailer@example.com");

        User user = User.builder()
                .email("mail.verify@example.com")
                .firstName("Mai")
                .build();

        emailService.sendVerificationEmail(user, "verify-token");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String html = (String) captor.getValue().getContent();

        assertThat(captor.getValue().getSubject()).isEqualTo("Xác thực tài khoản - Old Bicycles Marketplace");
        assertThat(html).contains("Xin chào Mai!");
        assertThat(html).contains("Xác thực Email");
        assertThat(html).contains("https://public.example.com/api/auth/verify-email?token=verify-token");
    }

    @Test
    void sendPasswordResetEmailUsesExternalTemplateWithVietnameseContent() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://public.example.com");
        ReflectionTestUtils.setField(emailService, "fromEmail", "mailer@example.com");

        User user = User.builder()
                .email("mail.reset@example.com")
                .firstName("Ngọc")
                .build();

        emailService.sendPasswordResetEmail(user, "reset-token");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String html = (String) captor.getValue().getContent();

        assertThat(captor.getValue().getSubject()).isEqualTo("Đặt lại mật khẩu - Old Bicycles Marketplace");
        assertThat(html).contains("Xin chào Ngọc!");
        assertThat(html).contains("Đặt lại mật khẩu");
        assertThat(html).contains("https://public.example.com/reset-password?token=reset-token");
    }

    @Test
    void createVerificationTokenDeletesOldTokenAndCreatesNewOne() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("mail.verify@example.com")
                .build();

        when(emailVerificationRepository.save(any(EmailVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerification verification = emailService.createVerificationToken(user);

        verify(emailVerificationRepository).deleteByUser(user);
        assertThat(verification.getUser()).isEqualTo(user);
        assertThat(verification.getToken()).isNotBlank();
        assertThat(verification.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    void createPasswordResetTokenDeletesOldTokenAndCreatesNewOne() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("mail.reset@example.com")
                .build();
        ReflectionTestUtils.setField(emailService, "passwordResetExpiration", 3_600_000L);

        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetToken resetToken = emailService.createPasswordResetToken(user);

        verify(passwordResetTokenRepository).deleteByUser(user);
        assertThat(resetToken.getUser()).isEqualTo(user);
        assertThat(resetToken.getToken()).isNotBlank();
        assertThat(resetToken.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(59));
    }
}
