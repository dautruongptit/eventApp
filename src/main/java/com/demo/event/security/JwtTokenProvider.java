package com.demo.event.security;

import com.demo.event.model.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * JWT Token Provider — JJWT 0.12.3.
 *
 * Cập nhật v2.1.0 (Ch.19.8):
 *   - generateAccessToken() nhận thêm Set<Role>, nhúng roles vào JWT claim
 *   - Thêm getRoles() để đọc roles từ token
 *
 * Bảng so sánh API JJWT 0.11.5 → 0.12.3:
 *   .setSubject()         → .subject()
 *   .setExpiration()      → .expiration()
 *   parserBuilder()       → parser()
 *   .setSigningKey()      → .verifyWith()
 *   .parseClaimsJws()     → .parseSignedClaims()
 *   .getBody()            → .getPayload()
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long accessExpiration;   // 24h

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;  // 7 days
    // ── KEY ─────────────────────────────────────────────────────────────

    /**
     * Tạo SecretKey từ chuỗi secret trong application.yml.
     * Secret phải dài >= 32 ký tự (256 bit) cho HMAC-SHA256.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── GENERATE ACCESS TOKEN ────────────────────────────────────────────

    /**
     * Tạo Access Token với roles nhúng vào payload (JJWT 0.12.3).
     * Subject = userId (String). Claim "type" = "access".
     * Claim "roles" = ["ROLE_USER"] hoặc ["ROLE_USER","ROLE_ADMIN"].
     *
     * @param userId ID người dùng
     * @param roles  Danh sách role của user (từ DB hoặc SecurityContext)
     */
    public String generateAccessToken(Long userId, Set<Role> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessExpiration);

        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type",  "access")
                .claim("roles", roleNames)          // nhúng roles vào payload
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ── GENERATE REFRESH TOKEN ───────────────────────────────────────────

    /**
     * Tạo Refresh Token.
     * Claim "type" = "refresh" để phân biệt với access token.
     * Không nhúng roles — refresh token chỉ dùng để lấy access token mới.
     */
    public String generateRefreshToken(Long userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ── GET USER ID ──────────────────────────────────────────────────────

    /**
     * Extract userId từ JWT token.
     */
    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    // ── GET TOKEN TYPE ───────────────────────────────────────────────────

    /**
     * Kiểm tra token là "access" hay "refresh".
     * Dùng trong AuthService.refreshToken() để tránh dùng access token để refresh.
     */
    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("type", String.class);
    }

    // ── GET ROLES ────────────────────────────────────────────────────────

    /**
     * Đọc danh sách tên role từ JWT claim "roles".
     * Dùng trong JwtAuthFilter để set authorities vào SecurityContext
     * mà không cần query DB.
     *
     * @return danh sách tên role, VD: ["ROLE_USER"] hoặc ["ROLE_USER","ROLE_ADMIN"]
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return (List<String>) claims.get("roles");
    }

    // ── VALIDATE TOKEN ───────────────────────────────────────────────────

    /**
     * Kiểm tra token hợp lệ: chữ ký đúng, chưa hết hạn, đúng định dạng.
     *
     * @return true  = token hợp lệ
     *         false = token không hợp lệ (hết hạn, giả mạo, sai cú pháp...)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token đã hết hạn: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("[JWT] Chữ ký không hợp lệ: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Token bị lỗi định dạng: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] Loại token không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] Token rỗng hoặc null: {}", e.getMessage());
        }
        return false;
    }

}
