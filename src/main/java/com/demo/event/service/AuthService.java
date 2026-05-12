package com.demo.event.service;

import com.demo.event.exception.BadRequestException;
import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.request.UpdateSettingsRequest;
import com.demo.event.model.dto.response.AuthResponse;
import com.demo.event.model.dto.response.UserProfileResponse;
import com.demo.event.model.entity.User;
import com.demo.event.repository.UserRepository;
import com.demo.event.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder     passwordEncoder;
    private final JwtTokenProvider jwtProvider;

    // ── REGISTER ────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email này đã được sử dụng");

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .language("vi")
                .darkMode(false)
                .isActive(true)
                .totalEvents(0)
                .totalRelatives(0)
                .build();

        User saved = userRepo.save(user);
        return buildAuthResponse(saved);
    }

    // ── LOGIN ───────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmailAndIsActiveTrue(req.getEmail())
                .orElseThrow(() ->
                        new BadRequestException("Email không tồn tại hoặc đã bị khoá"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new BadRequestException("Mật khẩu không chính xác");

        return buildAuthResponse(user);
    }

    // ── REFRESH TOKEN ───────────────────────────────────────────────────
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken))
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn");

        Long userId = jwtProvider.getUserId(refreshToken);
        User user   = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        return buildAuthResponse(user);
    }

    // ── GET PROFILE ─────────────────────────────────────────────────────
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return toProfileResponse(user);
    }

    // ── UPDATE PROFILE ──────────────────────────────────────────────────
    @Transactional
    public UserProfileResponse updateProfile(Long userId, RegisterRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setFullName(req.getFullName());
        // Không cho đổi email qua endpoint này
        return toProfileResponse(userRepo.save(user));
    }

    // ── UPDATE SETTINGS (language, darkMode) ────────────────────────────
    @Transactional
    public UserProfileResponse updateSettings(Long userId, UpdateSettingsRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (req.getLanguage() != null)  user.setLanguage(req.getLanguage());
        if (req.getDarkMode() != null)  user.setDarkMode(req.getDarkMode());
        return toProfileResponse(userRepo.save(user));
    }

    // ── UPLOAD AVATAR ───────────────────────────────────────────────────
    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file)
            throws IOException {
        if (file.isEmpty())
            throw new BadRequestException("File ảnh không được để trống");

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadDir  = Paths.get("uploads/avatars");
        Files.createDirectories(uploadDir);
        Files.copy(file.getInputStream(), uploadDir.resolve(filename));

        User user = userRepo.findById(userId).orElseThrow();
        user.setAvatarUrl("/uploads/avatars/" + filename);
        return toProfileResponse(userRepo.save(user));
    }

    // ── CONNECT GOOGLE CALENDAR ─────────────────────────────────────────
    @Transactional
    public void connectGoogleCalendar(Long userId, String authCode) {
        // TODO: exchange authCode with Google OAuth2 for token
        User user = userRepo.findById(userId).orElseThrow();
        user.setGoogleCalendarToken(authCode); // lưu token sau khi exchange
        userRepo.save(user);
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .accessToken(jwtProvider.generateAccessToken(user.getId()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId()))
                .build();
    }

    private UserProfileResponse toProfileResponse(User u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .language(u.getLanguage())
                .darkMode(u.getDarkMode())
                .totalEvents(u.getTotalEvents())
                .totalRelatives(u.getTotalRelatives())
                .googleCalendarConnected(u.getGoogleCalendarToken() != null)
                .createdAt(u.getCreatedAt())
                .build();
    }
}
