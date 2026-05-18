package com.demo.event.controller;

import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.request.UpdateSettingsRequest;
import com.demo.event.model.dto.response.ApiResponse;
import com.demo.event.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(req)));
    }

    // POST /api/v1/auth/login
    public ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.login(req, httpRequest)));
    }


    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ResponseEntity.ok(
                ApiResponse.success(authService.refreshToken(refreshToken)));
    }

    // POST /api/v1/auth/logout  (client xoá token phía client)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Dang xuat thanh cong"));
    }

    // ── Profile endpoints (dùng prefix /users/me thực tế) ──────────────
    // GET /api/v1/users/me  →  đặt trong UserController riêng
    // PUT /api/v1/users/me
    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<?>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.updateProfile(userId, req)));
    }

    // PUT /api/v1/users/me/settings
    @PutMapping("/users/me/settings")
    public ResponseEntity<ApiResponse<?>> updateSettings(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateSettingsRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.updateSettings(userId, req)));
    }

    // PUT /api/v1/users/me/avatar  (multipart upload)
    @PutMapping(value = "/users/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(
                ApiResponse.success(authService.uploadAvatar(userId, file)));
    }

    // POST /api/v1/users/me/google-calendar
    @PostMapping("/users/me/google-calendar")
    public ResponseEntity<ApiResponse<?>> connectGoogleCalendar(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        authService.connectGoogleCalendar(userId, body.get("code"));
        return ResponseEntity.ok(ApiResponse.success("Ket noi Google Calendar thanh cong"));
    }
}
