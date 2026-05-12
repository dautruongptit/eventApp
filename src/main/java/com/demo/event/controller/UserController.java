package com.demo.event.controller;

import com.demo.event.model.dto.response.ApiResponse;
import com.demo.event.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

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
}
