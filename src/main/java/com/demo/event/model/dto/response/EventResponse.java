package com.demo.event.model.dto.response;

import com.demo.event.model.entity.Event;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String title;

    // Danh mục sự kiện (thay cho enum EventType cũ — SEC: Danh mục sự kiện)
    private Long categoryId;
    private String categoryCode;     // SINH_NHAT, KY_NIEM...
    private String categoryName;     // "Sinh nhật"
    private String categoryIcon;     // "cake"
    private String categoryColor;    // "#FF6B6B"

    private java.time.LocalDate eventDate;
    private java.time.LocalTime eventTime;
    private Boolean isRecurring;
    private String recurrenceType;
    private String notes;
    // Thông tin người thân (null nếu là sự kiện bản thân)
    private Long relativeId;
    private String relativeName;
    private String relativeGroupType;
    // Countdown
    private Long daysUntil;  // âm = đã qua, 0 = hôm nay, dương = còn x ngày
    // Danh sách reminder config
    private List<ReminderResponse> reminders;

    private List<ParticipantSummary> participants;

    /** Map các trường vô hướng từ Event entity. Reminders & participants do service bổ sung. */
    public static EventResponse from(Event e, LocalDate today) {
        return EventResponse.builder()
            .id(e.getId())
            .title(e.getTitle())
            .categoryId(e.getCategory() != null ? e.getCategory().getId() : null)
            .categoryCode(e.getCategory() != null ? e.getCategory().getCode() : null)
            .categoryName(e.getCategory() != null ? e.getCategory().getDisplayName() : null)
            .categoryIcon(e.getCategory() != null ? e.getCategory().getIcon() : null)
            .categoryColor(e.getCategory() != null ? e.getCategory().getColorHex() : null)
            .eventDate(e.getEventDate())
            .eventTime(e.getEventTime())
            .isRecurring(e.getIsRecurring())
            .recurrenceType(e.getRecurrenceType() != null ? e.getRecurrenceType().name() : null)
            .notes(e.getNotes())
            .relativeId(e.getRelative() != null ? e.getRelative().getId() : null)
            .relativeName(e.getRelative() != null ? e.getRelative().getName() : null)
            .relativeGroupType(e.getRelative() != null && e.getRelative().getGroupType() != null
                ? e.getRelative().getGroupType().name() : null)
            .daysUntil(ChronoUnit.DAYS.between(today, e.getEventDate()))
            .build();
    }

    @Data
    @Builder
    public static class ParticipantSummary {
        private Long id;
        private String name;
        private String avatarUrl;
    }
}
