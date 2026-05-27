package com.demo.event.controller;
import com.demo.event.model.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Hidden   // An toan bo controller khoi Swagger UI
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    /**
     * GET /internal/health-check
     * Kiem tra trang thai: app + DB connection.
     * Dung cho: Docker HEALTHCHECK, load balancer probe,
     *           uptime monitoring (UptimeRobot, Pingdom...).
     */
    @GetMapping("/health-check")
    public ResponseEntity<BaseResponse<Map<String, Object>>> healthCheck() {

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("app",       "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("version",   "2.3.0");

        // Kiem tra DB connection
        String dbStatus;
        try (Connection conn = dataSource.getConnection()) {
            dbStatus = conn.isValid(2) ? "UP" : "DEGRADED";
        } catch (Exception e) {
            dbStatus = "DOWN";
        }
        status.put("database", dbStatus);

        // HTTP 200 neu app UP (du DB co DEGRADED)
        // HTTP 503 neu DB DOWN hoan toan
        if ("DOWN".equals(dbStatus)) {
            return ResponseEntity.status(503)
                    .body(BaseResponse.error("Service unavailable: DB connection failed"));
        }

        return ResponseEntity.ok(BaseResponse.success(status, "Service is healthy"));
    }
}
