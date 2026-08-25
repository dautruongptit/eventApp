package com.demo.event.filter;

import com.demo.event.util.DeviceParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Log chi tiet moi HTTP request/response: method, URI, IP, status, thoi gian
 * xu ly, va body (o muc DEBUG, da mask field nhay cam).
 *
 * Chay TRUOC ca Spring Security filter chain (order = HIGHEST_PRECEDENCE) de
 * requestId trong MDC bao trum toan bo log cua 1 request, ke ca log phat sinh
 * trong JwtAuthFilter / GlobalExceptionHandler.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_REQUEST_ID = "requestId";
    private static final int MAX_BODY_LOG_LENGTH = 2000;

    // Cac field nhay cam khong duoc in ra log — mask gia tri trong JSON body
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
        "(?i)\"(password|newPassword|oldPassword|token|accessToken|refreshToken|idToken|secret)\"\\s*:\\s*\"[^\"]*\"");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_REQUEST_ID, requestId);

        // Bo qua wrap body cho multipart (upload avatar) — tranh doc het stream
        // truoc khi Spring MultipartResolver kip xu ly.
        boolean isMultipart = request.getContentType() != null
            && request.getContentType().toLowerCase().startsWith("multipart/");

        ContentCachingRequestWrapper  wrappedRequest  = isMultipart
            ? null : new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        try {
            log.info("[Request] --> {} {} ip={}",
                request.getMethod(), buildUri(request), DeviceParser.getClientIp(request));

            filterChain.doFilter(wrappedRequest != null ? wrappedRequest : request, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (wrappedRequest != null && log.isDebugEnabled()) {
                String body = maskSensitive(getContentAsString(wrappedRequest.getContentAsByteArray()));
                if (StringUtils.hasText(body)) {
                    log.debug("[Request] body {} {}: {}", request.getMethod(), buildUri(request), body);
                }
            }

            log.info("[Request] <-- {} {} status={} duration={}ms",
                request.getMethod(), buildUri(request), wrappedResponse.getStatus(), duration);

            wrappedResponse.copyBodyToResponse();
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String buildUri(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();
    }

    private String getContentAsString(byte[] content) {
        if (content == null || content.length == 0) return "";
        String body = new String(content, StandardCharsets.UTF_8);
        return body.length() > MAX_BODY_LOG_LENGTH
            ? body.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated)" : body;
    }

    private String maskSensitive(String body) {
        if (!StringUtils.hasText(body)) return body;
        return SENSITIVE_FIELD_PATTERN.matcher(body).replaceAll("\"$1\":\"***\"");
    }
}
