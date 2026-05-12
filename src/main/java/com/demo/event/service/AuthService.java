package com.demo.event.service;

import com.demo.event.exception.BadRequestException;
import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.response.AuthResponse;
import com.demo.event.model.entity.User;
import com.demo.event.repository.UserRepository;
import com.demo.event.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new com.demo.event.exception.BadRequestException("Email đã được sử dụng");

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .isActive(true)
                .build();

        User saved = userRepo.save(user);

        return AuthResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .accessToken(jwtProvider.generateAccessToken(saved.getId()))
                .refreshToken(jwtProvider.generateRefreshToken(saved.getId()))
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmailAndIsActiveTrue(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Email không tồn tại"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new BadRequestException("Mật khẩu không chính xác");

        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .accessToken(jwtProvider.generateAccessToken(user.getId()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId()))
                .build();
    }
}
