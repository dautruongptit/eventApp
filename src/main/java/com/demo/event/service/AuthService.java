package com.demo.event.service;

import com.demo.event.exception.BadRequestException;
import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.request.UpdateSettingsRequest;
import com.demo.event.model.dto.response.AuthResponse;
import com.demo.event.model.dto.response.UserProfileResponse;
import com.demo.event.model.entity.LoginHistory;
import com.demo.event.model.entity.Role;
import com.demo.event.model.entity.User;
import com.demo.event.repository.LoginHistoryRepository;
import com.demo.event.repository.RoleRepository;
import com.demo.event.repository.UserRepository;
import com.demo.event.security.JwtTokenProvider;
import com.demo.event.util.DeviceParser;
import com.demo.event.util.MessageHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepo;
    private final PasswordEncoder     passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final LoginHistoryRepository loginHistoryRepo;
    private final DeviceParser deviceParser;
    private final MessageHelper messageHelper;
    // === CONSTANT: giới hạn nghiệp vụ ===
    private static final int    MAX_FAILED_ATTEMPTS  = 5;          // khoa sau 5 lan sai
    private static final long   LOCK_DURATION_MINUTES = 30L;       // khoa 30 phut

    // ── REGISTER ────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email này đã được sử dụng");

        // Lấy ROLE_USER từ DB
        Role userRole = roleRepository
                .findByName(Role.RoleName.ROLE_USER.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role USER chưa được khởi tạo"));

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .language("vi")
                .darkMode(false)
                .isActive(true)
                .totalEvents(0)
                .totalRelatives(0)
                .roles(new HashSet<>(Set.of(userRole)))     // gán ROLE_USER
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
                .accessToken(jwtProvider.generateAccessToken(user.getId(), user.getRoles()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId()))
                .build();
    }
// ── DEACTIVATE USER (Admin only) ────────────────────────────────────

    /**
     * Khoá tài khoản user (chỉ Admin được gọi endpoint này).
     * Endpoint: PUT /api/v1/users/{id}/deactivate
     * Phân quyền: @PreAuthorize("hasRole('ADMIN')") trong UserController
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + userId));
        user.setIsActive(false);
        userRepo.save(user);
    }
    // ── GRANT ADMIN ROLE (Admin only) ────────────────────────────────────

    /**
     * Cấp role ADMIN cho một user cụ thể.
     * Endpoint: PUT /api/v1/users/{id}/grant-admin
     * Phân quyền: @PreAuthorize("hasRole('ADMIN')") trong UserController
     */
    @Transactional
    public void grantAdminRole(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + userId));

        Role adminRole = roleRepository
                .findByName(Role.RoleName.ROLE_ADMIN.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role ADMIN chưa được khởi tạo"));

        user.getRoles().add(adminRole);
        userRepo.save(user);
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

    // === PHƯƠNG THỨC LOGIN ĐÃ CẬP NHẬT ===
    public AuthResponse login(LoginRequest req,
                              HttpServletRequest httpRequest) {

        // 1. Tim user theo email
        User user = userRepo.findByEmail(req.getEmail())
                .orElse(null);

        // 2. Email khong ton tai → luu lich su that bai an danh
        if (user == null) {
            throw new BadRequestException(
                    messageHelper.get("auth.email.not.found"));
        }

        // 3. Tai khoan bi vo hieu hoa / khoa vinh vien / da xoa
        if (!user.canLogin()) {
            LoginHistory.FailureReason reason =
                    switch (user.getStatus()) {
                        case LOCKED  -> LoginHistory.FailureReason.ACCOUNT_LOCKED;
                        case BANNED, DELETED, INACTIVE -> LoginHistory.FailureReason.ACCOUNT_INACTIVE;
                        default      -> LoginHistory.FailureReason.ACCOUNT_INACTIVE;
                    };
            saveLoginHistory(user, httpRequest, false, reason);
            throw new BadRequestException(
                    messageHelper.get("auth.account.status." + user.getStatus().getCode()));
        }

        // 4. Kiem tra tai khoan bi khoa tam thoi
        if (user.isCurrentlyLocked()) {
            saveLoginHistory(user, httpRequest, false,
                    LoginHistory.FailureReason.ACCOUNT_LOCKED);
            throw new BadRequestException(
                    messageHelper.get("auth.account.locked",
                            user.getMinutesUntilUnlock()));
        }

        // 5. Kiem tra mat khau
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, httpRequest);
            // Sau khi xu ly, nem loi (co the bao gom thong bao so lan con lai)
            int remaining = MAX_FAILED_ATTEMPTS - user.getFailedLoginCount();
            if (remaining <= 0) {
                throw new BadRequestException(
                        messageHelper.get("auth.account.just.locked",
                                LOCK_DURATION_MINUTES));
            }
            throw new BadRequestException(
                    messageHelper.get("auth.password.incorrect.with.count",
                            remaining));
        }

        // 6. Dang nhap thanh cong → reset va cap nhat thong tin
        handleSuccessLogin(user, httpRequest);

        return buildAuthResponse(user);
    }
    // === XỬ LÝ ĐĂNG NHẬP SAI ===
    @Transactional
    public void handleFailedLogin(User user, HttpServletRequest req) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        user.setLastFailedAt(LocalDateTime.now());

        // Kiem tra co vuot nguong khoa khong
        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(
                    LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }

        userRepo.save(user);
        saveLoginHistory(user, req, false,
                LoginHistory.FailureReason.WRONG_PASSWORD);
    }

    // === XỬ LÝ ĐĂNG NHẬP THÀNH CÔNG ===
    @Transactional
    public void handleSuccessLogin(User user, HttpServletRequest req) {
        // Reset dem that bai
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastFailedAt(null);
        // Cap nhat thong tin dang nhap
        user.setLastLoginAt(LocalDateTime.now());
        user.setTotalLoginCount(user.getTotalLoginCount() + 1);
        user.setLastLoginIp(deviceParser.extractIp(req));
        userRepo.save(user);
        saveLoginHistory(user, req, true, null);
    }

    // === LƯU LỊCH SỬ ĐĂNG NHẬP ===
    private void saveLoginHistory(User user,
                                  HttpServletRequest req,
                                  boolean success,
                                  LoginHistory.FailureReason reason) {
        String ua    = req.getHeader("User-Agent");
        String ip    = deviceParser.extractIp(req);

        LoginHistory history = LoginHistory.builder()
                .user(user)
                .ipAddress(ip)
                .userAgent(ua)
                .deviceType(deviceParser.parseDeviceType(ua))
                .os(deviceParser.parseOs(ua))
                .browser(deviceParser.parseBrowser(ua))
                .isSuccess(success)
                .failureReason(reason)
                .loginAt(LocalDateTime.now())
                .build();

        loginHistoryRepo.save(history);
    }

}
