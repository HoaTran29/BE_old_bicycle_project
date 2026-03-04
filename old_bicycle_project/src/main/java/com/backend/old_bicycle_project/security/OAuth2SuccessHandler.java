package com.backend.old_bicycle_project.security;

import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.UserStatus;
import com.backend.old_bicycle_project.repository.UserRepository;
import com.backend.old_bicycle_project.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String avatarUrl = oAuth2User.getAttribute("picture");

        if (email == null) {
            log.error("Google OAuth2: email is null");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from Google");
            return;
        }

        // Tìm hoặc tạo user mới từ Google account
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .passwordHash("") // Google login không cần password
                            .firstName(firstName)
                            .lastName(lastName)
                            .avatarUrl(avatarUrl)
                            .role(AppRole.buyer)
                            .isVerified(true) // Google đã xác thực email
                            .status(UserStatus.active)
                            .build();
                    return userRepository.save(newUser);
                });

        // Tạo JWT tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        // Trả về JSON response
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);
        responseBody.put("tokenType", "Bearer");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("role", user.getRole());
        userInfo.put("isVerified", user.isVerified());
        responseBody.put("user", userInfo);

        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}
