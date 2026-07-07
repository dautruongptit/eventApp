package com.demo.event.model.entity;
import com.demo.event.model.converter.UserStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    /**
     * Danh sach role cua user.
     * FetchType.EAGER: load roles ngay khi load User
     * (can thiet cho Spring Security UserDetails).
     * CascadeType.MERGE: khi save user thi merge roles.
     */
    @ManyToMany(fetch = FetchType.EAGER,
            cascade = { CascadeType.MERGE })
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    /** Helper: kiem tra user co role cu the khong. */
    public boolean hasRole(Role.RoleName roleName) {
        return roles.stream()
                .anyMatch(r -> r.getName().equals(roleName.name()));
    }

    /**
     * Trang thai tai khoan theo UserStatus enum.
     * Luu ma 3 ky tu: REG, VRF, ACT, INA, LCK, BAN, DEL.
     * Thay the TINYINT is_active cu.
     */
    @Convert(converter = UserStatusConverter.class)
    @Column(nullable = false, length = 3, columnDefinition = "VARCHAR(3) DEFAULT 'REG'")
    @Builder.Default
    private UserStatus status = UserStatus.REGISTERED;

    // ── Login tracking (Ch.20) ───────────────────────────────────────────
    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "total_login_count", nullable = false)
    @Builder.Default
    private Integer totalLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    public boolean isCurrentlyLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public long getMinutesUntilUnlock() {
        if (!isCurrentlyLocked()) return 0;
        return java.time.temporal.ChronoUnit.MINUTES
                .between(LocalDateTime.now(), lockedUntil);
    }

    /** Kiem tra status co phai active / hoat dong binh thuong. */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /** Kiem tra tai khoan co the dang nhap (ACT hoac VRF). */
    public boolean canLogin() {
        return status == UserStatus.ACTIVE
                || status == UserStatus.VERIFIED;
    }


}
