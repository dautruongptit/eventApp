package com.demo.event.controller;

import com.demo.event.model.dto.request.GoogleLoginRequest;
import com.demo.event.model.dto.request.LoginRequest;
import com.demo.event.model.dto.request.RefreshTokenRequest;
import com.demo.event.model.dto.request.RegisterRequest;
import com.demo.event.model.dto.response.BaseResponse;
import com.demo.event.service.AuthService;
import com.demo.event.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, refresh token, logout")
public class AuthController {

    private final AuthService        authService;
    private final GoogleAuthService  googleAuthService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<BaseResponse<?>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success(authService.register(req)));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng email/mật khẩu")
    public ResponseEntity<BaseResponse<?>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
            BaseResponse.success(authService.login(req, httpRequest)));
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập / đăng ký bằng Google",
               description = "Client gửi idToken lấy từ Google Sign-In SDK.")
    public ResponseEntity<BaseResponse<?>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest req,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
            BaseResponse.success(googleAuthService.loginWithGoogle(req.getIdToken(), httpRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token")
    public ResponseEntity<BaseResponse<?>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(
            BaseResponse.success(authService.refreshToken(req.getRefreshToken())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất (stateless — client tự xóa token)")
    public ResponseEntity<BaseResponse<?>> logout() {
        return ResponseEntity.ok(BaseResponse.success(null, "Đăng xuất thành công"));
    }
}
