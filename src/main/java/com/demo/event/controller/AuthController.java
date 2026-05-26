package com.demo.event.controller;

import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.request.UpdateSettingsRequest;
import com.demo.event.model.dto.response.BaseResponse;
import com.demo.event.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Dang ky, dang nhap, refresh token, logout")
public class AuthController {

    private final AuthService authService;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<?>> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(authService.register(req)));
    }

    // POST /api/v1/auth/login
    public ResponseEntity<BaseResponse<?>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                BaseResponse.success(authService.login(req, httpRequest)));
    }


    // POST /api/v1/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ResponseEntity.ok(
                BaseResponse.success(authService.refreshToken(refreshToken)));
    }

    // POST /api/v1/auth/logout  (client xoá token phía client)
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout() {
        return ResponseEntity.ok(BaseResponse.success("Dang xuat thanh cong"));
    }

    // ── Profile endpoints (dùng prefix /users/me thực tế) ──────────────
    // GET /api/v1/users/me  →  đặt trong UserController riêng
    // PUT /api/v1/users/me
    @PutMapping("/users/me")
    public ResponseEntity<BaseResponse<?>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(
                BaseResponse.success(authService.updateProfile(userId, req)));
    }

    // PUT /api/v1/users/me/settings
    @PutMapping("/users/me/settings")
    public ResponseEntity<BaseResponse<?>> updateSettings(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateSettingsRequest req) {
        return ResponseEntity.ok(
                BaseResponse.success(authService.updateSettings(userId, req)));
    }

    // PUT /api/v1/users/me/avatar  (multipart upload)
    @PutMapping(value = "/users/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<?>> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(
                BaseResponse.success(authService.uploadAvatar(userId, file)));
    }

    // POST /api/v1/users/me/google-calendar
    @PostMapping("/users/me/google-calendar")
    public ResponseEntity<BaseResponse<?>> connectGoogleCalendar(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        authService.connectGoogleCalendar(userId, body.get("code"));
        return ResponseEntity.ok(BaseResponse.success("Ket noi Google Calendar thanh cong"));
    }
}
