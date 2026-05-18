package com.demo.event.controller;

import com.demo.event.model.dto.response.ApiResponse;
import com.demo.event.model.dto.response.LoginHistoryResponse;
import com.demo.event.repository.LoginHistoryRepository;
import com.demo.event.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final LoginHistoryRepository loginHistoryRepo;
    /**
     * GET /api/v1/users/me
     * Trả về thông tin profile đầy đủ của user đang đăng nhập.
     * Bao gồm: totalEvents, totalRelatives, daysUntilNextEvent,
     *          language, darkMode, googleCalendarConnected.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMe(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.getProfile(userId)));
    }

    /**
     * GET /api/v1/users/me/login-history?page=0&size=10
     * User xem lich su dang nhap cua chinh minh.
     * Bao gom: IP, thiet bi, OS, trinh duyet, ket qua, thoi gian.
     */
    @GetMapping("/me/login-history")
    public ResponseEntity<ApiResponse<?>> getLoginHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        var history = loginHistoryRepo
                .findByUserIdOrderByLoginAtDesc(userId, PageRequest.of(page, size))
                .map(h -> LoginHistoryResponse.builder()
                        .id(h.getId())
                        .ipAddress(h.getIpAddress())
                        .deviceType(h.getDeviceType())
                        .os(h.getOs())
                        .browser(h.getBrowser())
                        .country(h.getCountry())
                        .isSuccess(h.getIsSuccess())
                        .failureReason(h.getFailureReason() != null
                                ? h.getFailureReason().name() : null)
                        .loginAt(h.getLoginAt())
                        .build());

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * GET /api/v1/admin/login-history/{userId}    (ADMIN only)
     * Admin xem lich su dang nhap cua bat ky user.
     */
    @GetMapping("/{id}/login-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getUserLoginHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var history = loginHistoryRepo
                .findByUserIdOrderByLoginAtDesc(id, PageRequest.of(page, size))
                .map(h -> LoginHistoryResponse.builder()
                        .id(h.getId()).ipAddress(h.getIpAddress())
                        .deviceType(h.getDeviceType()).os(h.getOs())
                        .browser(h.getBrowser()).isSuccess(h.getIsSuccess())
                        .failureReason(h.getFailureReason() != null
                                ? h.getFailureReason().name() : null)
                        .loginAt(h.getLoginAt()).build());

        return ResponseEntity.ok(ApiResponse.success(history));
    }

}
