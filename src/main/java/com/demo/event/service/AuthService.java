package com.demo.event.service;

import com.demo.event.exception.BadRequestException;
import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.exception.UnauthorizedException;
import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.request.UpdateProfileRequest;
import com.demo.event.model.dto.request.UpdateSettingsRequest;
import com.demo.event.model.dto.response.AuthResponse;
import com.demo.event.model.dto.response.LoginHistoryResponse;
import com.demo.event.model.dto.response.UserProfileResponse;
import com.demo.event.model.entity.LoginHistory;
import com.demo.event.model.entity.Role;
import com.demo.event.model.entity.User;
import com.demo.event.repository.LoginHistoryRepository;
import com.demo.event.repository.RoleRepository;
import com.demo.event.repository.UserRepository;
import com.demo.event.security.JwtTokenProvider;
import com.demo.event.util.DeviceParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final String UPLOAD_DIR = "uploads/avatars/";

    private final UserRepository         userRepo;
    private final RoleRepository         roleRepo;
    private final LoginHistoryRepository loginHistoryRepo;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;

    // ── REGISTER ──────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        Role userRole = roleRepo.findByName("ROLE_USER")
            .orElseThrow(() -> new ResourceNotFoundException("Role", "ROLE_USER"));

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .status("REG")
            .roles(Set.of(userRole))
            .build();

        userRepo.save(user);

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getRoles());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .userId(user.getId()).fullName(user.getFullName()).email(user.getEmail())
            .build();
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest httpRequest) {
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không đúng"));

        String ip = DeviceParser.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (!user.canLogin()) {
            saveLoginHistory(user, ip, userAgent, false, LoginHistory.FailureReason.ACCOUNT_INACTIVE);
            throw new UnauthorizedException("Tài khoản chưa được kích hoạt");
        }

        if (user.isCurrentlyLocked()) {
            saveLoginHistory(user, ip, userAgent, false, LoginHistory.FailureReason.ACCOUNT_LOCKED);
            throw new UnauthorizedException(
                "Tài khoản đang bị khóa, thử lại sau " + user.getMinutesUntilUnlock() + " phút");
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            saveLoginHistory(user, ip, userAgent, false, LoginHistory.FailureReason.WRONG_PASSWORD);
            throw new UnauthorizedException("Email hoặc mật khẩu không đúng");
        }

        handleSuccessLogin(user, ip);
        saveLoginHistory(user, ip, userAgent, true, null);

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getRoles());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .userId(user.getId()).fullName(user.getFullName()).email(user.getEmail())
            .build();
    }

    // ── REFRESH TOKEN ─────────────────────────────────────────────────────────
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)
                || !"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String newAccessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getRoles());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(newAccessToken).refreshToken(newRefreshToken)
            .userId(user.getId()).fullName(user.getFullName()).email(user.getEmail())
            .build();
    }

    // ── PROFILE — cache 30 phút ──────────────────────────────────────────────
    @Cacheable(value = "userProfile", key = "#userId")
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserProfileResponse.from(user);
    }

    @CacheEvict(value = "userProfile", key = "#userId")
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setFullName(req.getFullName());
        return UserProfileResponse.from(userRepo.save(user));
    }

    @CacheEvict(value = "userProfile", key = "#userId")
    @Transactional
    public UserProfileResponse updateSettings(Long userId, UpdateSettingsRequest req) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (req.getLanguage() != null) user.setLanguage(req.getLanguage());
        if (req.getDarkMode() != null) user.setDarkMode(req.getDarkMode());
        return UserProfileResponse.from(userRepo.save(user));
    }

    @CacheEvict(value = "userProfile", key = "#userId")
    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), path);
            user.setAvatarUrl("/uploads/avatars/" + filename);
        } catch (IOException e) {
            throw new BadRequestException("Không thể upload avatar: " + e.getMessage());
        }

        return UserProfileResponse.from(userRepo.save(user));
    }

    // ── LOGIN HISTORY ─────────────────────────────────────────────────────────
    public Page<LoginHistoryResponse> getLoginHistory(Long userId, int page, int size) {
        return loginHistoryRepo.findByUserIdOrderByLoginAtDesc(userId, PageRequest.of(page, size))
            .map(LoginHistoryResponse::from);
    }

    // ── GOOGLE CALENDAR (placeholder — chưa implement logic đồng bộ thật) ──────
    @Transactional
    public void connectGoogleCalendar(Long userId, String authCode) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        // TODO: exchange authCode voi Google OAuth2 de lay token that (SEC backlog)
        user.setGoogleCalendarToken(authCode);
        userRepo.save(user);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────
    public Page<UserProfileResponse> getAllUsers(int page, int size) {
        return userRepo.findAll(PageRequest.of(page, size))
            .map(UserProfileResponse::from);
    }

    @Transactional
    public void deactivateUser(Long targetUserId, Long adminUserId) {
        if (targetUserId.equals(adminUserId)) {
            throw new BadRequestException("Không thể tự khóa chính mình");
        }
        User user = userRepo.findById(targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));
        user.setStatus("INA");
        userRepo.save(user);
    }

    @Transactional
    public void grantAdminRole(Long targetUserId) {
        User user = userRepo.findById(targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));
        Role adminRole = roleRepo.findByName("ROLE_ADMIN")
            .orElseThrow(() -> new ResourceNotFoundException("Role", "ROLE_ADMIN"));
        user.getRoles().add(adminRole);
        userRepo.save(user);
    }

    // ── HELPERS — Login tracking ─────────────────────────────────────────────

    private void handleFailedLogin(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        user.setLastFailedAt(LocalDateTime.now());
        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }
        userRepo.save(user);
    }

    private void handleSuccessLogin(User user, String ip) {
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setTotalLoginCount(user.getTotalLoginCount() + 1);
        user.setLastLoginIp(ip);
        userRepo.save(user);
    }

    private void saveLoginHistory(User user, String ip, String userAgent,
                                   boolean success, LoginHistory.FailureReason reason) {
        LoginHistory history = LoginHistory.builder()
            .user(user)
            .ipAddress(ip)
            .userAgent(userAgent)
            .deviceType(DeviceParser.parseDeviceType(userAgent))
            .os(DeviceParser.parseOs(userAgent))
            .browser(DeviceParser.parseBrowser(userAgent))
            .isSuccess(success)
            .failureReason(reason)
            .build();
        loginHistoryRepo.save(history);
    }
}
