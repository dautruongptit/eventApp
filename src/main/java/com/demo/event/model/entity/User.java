package com.demo.event.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'vi'")
    private String language = "vi";

    @Column(name = "dark_mode", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean darkMode = false;

    @Column(name = "total_events")
    private Integer totalEvents = 0;

    @Column(name = "total_relatives")
    private Integer totalRelatives = 0;

    @Column(name = "google_calendar_token", columnDefinition = "TEXT")
    private String googleCalendarToken;

    @Column(name = "is_active", columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

}
