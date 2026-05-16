package com.demo.event.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
/**
 * JWT Authentication Filter.
 *
 * QUAN TRỌNG: Class này KHÔNG đánh dấu @Component.
 * Nếu đánh @Component, Spring sẽ tự scan và inject vào SecurityConfig,
 * đồng thời SecurityConfig cũng inject JwtAuthFilter → circular dependency.
 * Thay vào đó, SecurityConfig tự tạo instance bằng:
 *   new JwtAuthFilter(jwtTokenProvider, userDetailsService)
 *
 * Cập nhật v2.1.0 (Ch.19.9):
 *   - Đọc roles từ JWT claim (không cần query DB mỗi request)
 *   - Set List<SimpleGrantedAuthority> vào SecurityContext
 *   - Cho phép @PreAuthorize("hasRole('ADMIN')") hoạt động trên Controller
 *
 * Luồng xử lý:
 *   1. Đọc header Authorization: Bearer <token>
 *   2. Validate JWT bằng JwtTokenProvider
 *   3. Extract userId và roles từ token
 *   4. Tạo UsernamePasswordAuthenticationToken với authorities
 *   5. Set Authentication vào SecurityContextHolder
 */
// KHONG @Component — SecurityConfig tao bang new JwtAuthFilter(...)
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider    jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserId(token);

                // Doc roles tu JWT claim (khong can query DB)
                List<String> roleNames = jwtTokenProvider.getRoles(token);
                var authorities = roleNames.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Set principal la userId (Long) de @AuthenticationPrincipal van hoat dong)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,       // principal = userId
                                null,         // credentials
                                authorities   // roles tu JWT
                        );
                auth.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
    /** Trích xuất token từ header "Authorization: Bearer <token>" */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
