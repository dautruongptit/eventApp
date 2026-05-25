package com.demo.event.config;

import com.demo.event.security.CustomUserDetailsService;
import com.demo.event.security.JwtAuthFilter;
import com.demo.event.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
@EnableMethodSecurity   // bat @PreAuthorize / @PostAuthorize trong Controller
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF (REST API stateless, không dùng cookie session)
                .csrf(AbstractHttpConfigurer::disable)

                // Không tạo session (JWT tự quản lý state)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân quyền endpoint
                .authorizeHttpRequests(auth -> auth
                        // ── Public: đăng ký, đăng nhập, refresh token ──────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh").permitAll()

                        // ── Health check (cho load balancer / Docker) ────────────
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Tất cả còn lại: cần JWT hợp lệ (bất kỳ role) ─────────
                        // Phân quyền ADMIN/USER được xử lý bằng @PreAuthorize
                        // trực tiếp trên từng method trong UserController

                        // ── SWAGGER UI (chi mo khi SWAGGER_ENABLED=true) ──
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs",
                                "/api-docs/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated())

                // Thêm JWT filter TRƯỚC UsernamePasswordAuthenticationFilter
                // Tạo bằng "new" để tránh circular dependency
                .addFilterBefore(
                        new JwtAuthFilter(jwtTokenProvider, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }
    /**
     * AuthenticationManager bean — cần thiết khi dùng
     * DaoAuthenticationProvider hoặc inject vào AuthService.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCryptPasswordEncoder với strength = 12.
     * Strength 12 ~ 300ms/hash, đủ chậm để chống brute-force.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

}
