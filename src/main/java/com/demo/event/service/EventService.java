package com.demo.event.service;

import com.demo.event.exception.ForbiddenException;
import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.model.dto.request.CreateEventRequest;
import com.demo.event.model.dto.request.ReminderRequest;
import com.demo.event.model.dto.response.EventResponse;
import com.demo.event.model.dto.response.ReminderResponse;
import com.demo.event.model.entity.*;
import com.demo.event.repository.EventCategoryRepository;
import com.demo.event.repository.EventParticipantRepository;
import com.demo.event.repository.EventRepository;
import com.demo.event.repository.RelativeRepository;
import com.demo.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final EventParticipantRepository participantRepo;
    private final EventCategoryRepository categoryRepo;

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

    // ── GET LIST (filter đa điều kiện — dùng categoryId) ────────────────
    public List<EventResponse> getEvents(Long userId, Long categoryId,
                                         Long relativeId, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        return eventRepo
                .findFiltered(userId, categoryId, relativeId, month, year)
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

        EventCategory category = resolveCategory(req.getCategoryId());
        Relative relative = resolveRelative(req.getRelativeId(), userId);

        Event event = Event.builder()
                .user(user)
                .relative(relative)
                .title(req.getTitle())
                .category(category)
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
            event.setReminders(buildReminders(req.getReminders(), event));
        }

        Event saved = eventRepo.save(event);

        // Người thân khác cùng tham gia (event_participants)
        if (req.getParticipantIds() != null && !req.getParticipantIds().isEmpty()) {
            saveParticipants(saved, userId, req.getParticipantIds());
        }

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

        event.setRelative(resolveRelative(req.getRelativeId(), userId));
        event.setTitle(req.getTitle());
        event.setCategory(resolveCategory(req.getCategoryId()));
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

    private EventCategory resolveCategory(Long categoryId) {
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Danh muc su kien khong ton tai: " + categoryId));
    }

    private Relative resolveRelative(Long relativeId, Long userId) {
        if (relativeId == null) return null;
        return relativeRepo.findByIdAndUserId(relativeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nguoi than khong ton tai"));
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
                    .isEnabled(r.getIsEnabled() == null || r.getIsEnabled())
                    .build());
        }
        return result;
    }

    /** Map Event entity -> EventResponse DTO (kèm reminders + participants). */
    public EventResponse toResponse(Event e, LocalDate today) {
        EventResponse response = EventResponse.from(e, today);

        if (e.getReminders() != null && !e.getReminders().isEmpty()) {
            List<ReminderResponse> reminders = e.getReminders().stream()
                    .map(r -> ReminderResponse.builder()
                            .id(r.getId())
                            .remindDaysBefore(r.getRemindDaysBefore())
                            .remindHoursBefore(r.getRemindHoursBefore())
                            .isEnabled(r.getIsEnabled())
                            .build())
                    .collect(Collectors.toList());
            response.setReminders(reminders);
        }

        List<EventParticipant> participants = participantRepo.findByEventId(e.getId());
        if (!participants.isEmpty()) {
            response.setParticipants(participants.stream()
                    .map(ep -> EventResponse.ParticipantSummary.builder()
                            .id(ep.getRelative().getId())
                            .name(ep.getRelative().getName())
                            .avatarUrl(ep.getRelative().getAvatarUrl())
                            .build())
                    .toList());
        }
        return response;
    }

    private void saveParticipants(Event event, Long userId, List<Long> relativeIds) {
        List<Relative> relatives = relativeRepo.findAllById(relativeIds).stream()
                .filter(r -> r.getUser().getId().equals(userId))  // chi cho phep relative cua chinh user
                .toList();

        List<EventParticipant> participants = relatives.stream()
                .map(r -> EventParticipant.builder()
                        .event(event)
                        .relative(r)
                        .build())
                .toList();

        participantRepo.saveAll(participants);
    }
}
