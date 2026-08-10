package com.demo.event.model.dto.response;

import com.demo.event.model.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String body;
    private Boolean isRead;
    private Long eventId;
    private LocalDateTime sentAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .title(n.getTitle())
            .body(n.getBody())
            .isRead(n.getIsRead())
            .eventId(n.getEvent() != null ? n.getEvent().getId() : null)
            .sentAt(n.getSentAt())
            .build();
    }
}
