package com.demo.event.model.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String language;
    private Boolean darkMode;
    private Integer totalEvents;
    private Integer totalRelatives;
    private Integer daysUntilNextEvent; // tính từ DB
    private Boolean googleCalendarConnected;
    private LocalDateTime createdAt;
}
