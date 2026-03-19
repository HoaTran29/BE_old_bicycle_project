package com.backend.old_bicycle_project.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final UserDetailsService userDetailsService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        /**
         * Filter chain 1: Public endpoints — KHÔNG cần xác thực
         */
        @Bean
        @Order(1)
        public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatchers(matchers -> matchers.requestMatchers(
                                                // Auth public
                                                "/api/auth/register",
                                                "/api/auth/login",
                                                "/api/auth/refresh",
                                                "/api/auth/forgot-password",
                                                "/api/auth/reset-password",
                                                "/api/auth/verify-email",
                                                "/api/payments/sepay/webhook",
                                                // Swagger UI
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**",
                                                // OAuth2
                                                "/oauth2/**",
                                                "/login/oauth2/**"))
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .anyRequest().permitAll());

                return http.build();
        }

        /**
         * Filter chain 2: Protected endpoints — cần JWT hoặc OAuth2
         */
        @Bean
        @Order(2)
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // ===== Product public endpoints (GET only) =====
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/products",
                                                                "/api/products/*",
                                                                "/api/brands",
                                                                "/api/brands/*",
                                                                "/api/categories",
                                                                "/api/categories/*",
                                                                "/api/brake-types",
                                                                "/api/frame-materials",
                                                                "/api/users/*/reviews")
                                                .permitAll()

                                                // ===== WebSocket =====
                                                .requestMatchers("/ws/**").permitAll()

                                                // ===== Admin only =====
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // ===== Inspector =====
                                                .requestMatchers(HttpMethod.POST, "/api/inspections/request/*")
                                                .hasAnyRole("SELLER", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/inspections/evaluate/*")
                                                .hasAnyRole("INSPECTOR", "ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/api/inspections/report/*")
                                                .hasAnyRole("INSPECTOR", "ADMIN")
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/inspections/dashboard",
                                                                "/api/inspections/requests",
                                                                "/api/inspections/history")
                                                .hasAnyRole("INSPECTOR", "ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/inspections/**").permitAll()

                                                // ===== Seller =====
                                                .requestMatchers(HttpMethod.POST, "/api/products")
                                                .hasAnyRole("SELLER", "ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/products/*")
                                                .hasAnyRole("SELLER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH,
                                                                "/api/products/*/hide",
                                                                "/api/products/*/show")
                                                .hasAnyRole("SELLER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/api/products/*")
                                                .hasAnyRole("SELLER", "ADMIN")

                                                // ===== Buyer & Seller =====
                                                .requestMatchers("/api/orders/**")
                                                .hasAnyRole("BUYER", "SELLER", "ADMIN")

                                                .requestMatchers("/api/payout-profiles/**")
                                                .authenticated()

                                                // ===== Authenticated (any role) =====
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oAuth2SuccessHandler))
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"error\":\"Unauthorized\",\"message\":\"Bạn cần đăng nhập để truy cập API này\"}");
                                                }))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
