package com.demo.event.service;

import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.exception.UnauthorizedException;
import com.demo.event.model.dto.response.AuthResponse;
import com.demo.event.model.entity.Role;
import com.demo.event.model.entity.User;
import com.demo.event.repository.RoleRepository;
import com.demo.event.repository.UserRepository;
import com.demo.event.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.util.Set;

/**
 * SEC-27 — Dang nhap / Dang ky bang Google (Token Verify Flow).
 *
 * Flow:
 *   1. Verify idToken voi GoogleIdTokenVerifier (audience = GOOGLE_CLIENT_ID).
 *   2. googleId da co trong DB  -> login luon.
 *   3. email da co (tai khoan LOCAL) -> lien ket google_id vao tai khoan cu.
 *   4. email hoan toan moi     -> tao user moi, status = ACT (Google da xac minh email).
 *   5. Tra JWT access + refresh giong login thuong.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository         userRepo;
    private final RoleRepository         roleRepo;
    private final JwtTokenProvider       jwtTokenProvider;
    private final GoogleIdTokenVerifier  googleIdTokenVerifier;

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = verify(idToken);

        String googleId = payload.getSubject();
        String email    = payload.getEmail();
        String fullName = (String) payload.get("name");
        String picture  = (String) payload.get("picture");

        User user = userRepo.findByGoogleId(googleId)
            .orElseGet(() -> userRepo.findByEmail(email)
                .map(existing -> linkGoogleAccount(existing, googleId, picture))
                .orElseGet(() -> createGoogleUser(googleId, email, fullName, picture)));

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getId(), user.getRoles());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .userId(user.getId()).fullName(user.getFullName()).email(user.getEmail())
            .build();
    }

    // ── Verify idToken ────────────────────────────────────────────────────────
    private GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken token = googleIdTokenVerifier.verify(idToken);
            if (token == null) {
                throw new UnauthorizedException("Google idToken khong hop le hoac da het han");
            }
            return token.getPayload();
        } catch (GeneralSecurityException | java.io.IOException e) {
            log.warn("Google idToken verify failed: {}", e.getMessage());
            throw new UnauthorizedException("Khong the xac minh Google idToken");
        }
    }

    // ── Lien ket google_id vao tai khoan LOCAL da ton tai ────────────────────
    private User linkGoogleAccount(User existing, String googleId, String picture) {
        existing.setGoogleId(googleId);
        if (existing.getAvatarUrl() == null && picture != null) {
            existing.setAvatarUrl(picture);
        }
        return userRepo.save(existing);
    }

    // ── Tao user moi tu Google ────────────────────────────────────────────────
    private User createGoogleUser(String googleId, String email, String fullName, String picture) {
        Role userRole = roleRepo.findByName("ROLE_USER")
            .orElseThrow(() -> new ResourceNotFoundException("Role", "ROLE_USER"));

        User user = User.builder()
            .email(email)
            .fullName(fullName != null ? fullName : email)
            .googleId(googleId)
            .authProvider(User.AuthProvider.GOOGLE)
            .avatarUrl(picture)
            .status("ACT")                 // Google da xac minh email -> kich hoat luon
            .roles(Set.of(userRole))
            .build();

        return userRepo.save(user);
    }
}
