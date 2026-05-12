package com.demo.event.service;

import com.demo.event.exception.ForbiddenException;
import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.model.dto.request.CreateEventRequest;
import com.demo.event.model.dto.request.ReminderRequest;
import com.demo.event.model.dto.response.EventResponse;
import com.demo.event.model.dto.response.ReminderResponse;
import com.demo.event.model.entity.Event;
import com.demo.event.model.entity.EventReminder;
import com.demo.event.model.entity.Relative;
import com.demo.event.model.entity.User;
import com.demo.event.repository.EventRepository;
import com.demo.event.repository.RelativeRepository;
import com.demo.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepo;
    private final RelativeRepository relativeRepo;
    private final UserRepository userRepo;

    // ── GET UPCOMING (màn hình Home – tối đa limit sự kiện) ─────────────
    public List<EventResponse> getUpcoming(Long userId, int limit) {
        LocalDate today  = LocalDate.now();
        LocalDate future = today.plusDays(90);
        return eventRepo
                .findUpcoming(userId, today, future, PageRequest.of(0, limit))
                .stream()
                .map(e -> toResponse(e, today))
                .collect(Collectors.toList());
    }

    // ── GET LIST (filter đa điều kiện) ──────────────────────────────────
    public List<EventResponse> getEvents(Long userId, String typeStr,
                                         Long relativeId, Integer month, Integer year) {
        Event.EventType type = (typeStr != null && !typeStr.isBlank())
                ? Event.EventType.valueOf(typeStr) : null;
        LocalDate today = LocalDate.now();
        return eventRepo
                .findFiltered(userId, type, relativeId, month, year)
                .stream()
                .map(e -> toResponse(e, today))
                .collect(Collectors.toList());
    }

    // ── GET DETAIL ───────────────────────────────────────────────────────
    public EventResponse getById(Long id, Long userId) {
        Event e = findByIdAndOwner(id, userId);
        return toResponse(e, LocalDate.now());
    }

    // ── CREATE ───────────────────────────────────────────────────────────
    @Transactional
    public EventResponse create(Long userId, CreateEventRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nguoi dung khong ton tai"));

        // Resolve người thân (nullable – null = sự kiện bản thân)
        Relative relative = null;
        if (req.getRelativeId() != null) {
            relative = relativeRepo
                    .findByIdAndUserId(req.getRelativeId(), userId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Nguoi than khong ton tai"));
        }

        Event event = Event.builder()
                .user(user)
                .relative(relative)
                .title(req.getTitle())
                .eventType(Event.EventType.valueOf(req.getEventType()))
                .eventDate(req.getEventDate())
                .eventTime(req.getEventTime())
                .isRecurring(Boolean.TRUE.equals(req.getIsRecurring()))
                .recurrenceType(req.getRecurrenceType() != null
                        ? Event.RecurrenceType.valueOf(req.getRecurrenceType()) : null)
                .notes(req.getNotes())
                .isActive(true)
                .build();

        // Map reminders từ request
        if (req.getReminders() != null && !req.getReminders().isEmpty()) {
            List<EventReminder> reminders = buildReminders(req.getReminders(), event);
            event.setReminders(reminders);
        }

        Event saved = eventRepo.save(event);

        // Cập nhật cache counter
        userRepo.incrementEventCount(userId);
        if (relative != null)
            relativeRepo.incrementEventCount(relative.getId());

        return toResponse(saved, LocalDate.now());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────
    @Transactional
    public EventResponse update(Long id, Long userId, CreateEventRequest req) {
        Event event = findByIdAndOwner(id, userId);

        // Resolve người thân mới (có thể thay đổi)
        Relative newRelative = null;
        if (req.getRelativeId() != null) {
            newRelative = relativeRepo
                    .findByIdAndUserId(req.getRelativeId(), userId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Nguoi than khong ton tai"));
        }

        event.setRelative(newRelative);
        event.setTitle(req.getTitle());
        event.setEventType(Event.EventType.valueOf(req.getEventType()));
        event.setEventDate(req.getEventDate());
        event.setEventTime(req.getEventTime());
        event.setIsRecurring(Boolean.TRUE.equals(req.getIsRecurring()));
        event.setRecurrenceType(req.getRecurrenceType() != null
                ? Event.RecurrenceType.valueOf(req.getRecurrenceType()) : null);
        event.setNotes(req.getNotes());

        // Xoá reminders cũ, tạo lại từ request
        if (req.getReminders() != null) {
            event.getReminders().clear();
            event.getReminders().addAll(buildReminders(req.getReminders(), event));
        }

        return toResponse(eventRepo.save(event), LocalDate.now());
    }

    // ── DELETE (soft delete: isActive = false) ───────────────────────────
    @Transactional
    public void delete(Long id, Long userId) {
        Event event = findByIdAndOwner(id, userId);
        event.setIsActive(false);
        eventRepo.save(event);
        userRepo.decrementEventCount(userId);
        if (event.getRelative() != null)
            relativeRepo.decrementRelativeEventCount(event.getRelative().getId());
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────────────
    private Event findByIdAndOwner(Long id, Long userId) {
        Event e = eventRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Su kien khong ton tai: " + id));
        if (!e.getUser().getId().equals(userId))
            throw new ForbiddenException("Ban khong co quyen truy cap su kien nay");
        return e;
    }

    /** Build danh sách EventReminder từ request list. */
    private List<EventReminder> buildReminders(
            List<ReminderRequest> requests, Event event) {
        List<EventReminder> result = new ArrayList<>();
        for (ReminderRequest r : requests) {
            result.add(EventReminder.builder()
                    .event(event)
                    .remindDaysBefore(r.getRemindDaysBefore())
                    .remindHoursBefore(r.getRemindHoursBefore())
                    .isEnabled(Boolean.TRUE.equals(r.getIsEnabled()))
                    .build());
        }
        return result;
    }

    /** Map Event entity -> EventResponse DTO. */
    public EventResponse toResponse(Event e, LocalDate today) {
        long daysUntil = ChronoUnit.DAYS.between(today, e.getEventDate());

        List<ReminderResponse> reminders = (e.getReminders() == null)
                ? List.of()
                : e.getReminders().stream()
                .map(r -> ReminderResponse.builder()
                        .id(r.getId())
                        .remindDaysBefore(r.getRemindDaysBefore())
                        .remindHoursBefore(r.getRemindHoursBefore())
                        .isEnabled(r.getIsEnabled())
                        .build())
                .collect(Collectors.toList());

        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .eventType(e.getEventType().name())
                .eventDate(e.getEventDate())
                .eventTime(e.getEventTime())
                .isRecurring(e.getIsRecurring())
                .recurrenceType(e.getRecurrenceType() != null
                        ? e.getRecurrenceType().name() : null)
                .notes(e.getNotes())
                .relativeId(e.getRelative() != null ? e.getRelative().getId() : null)
                .relativeName(e.getRelative() != null ? e.getRelative().getName() : null)
                .relativeGroupType(e.getRelative() != null
                        ? e.getRelative().getGroupType().name() : null)
                .daysUntil(daysUntil)
                .reminders(reminders)
                .build();
    }
}

