package com.demo.event.service;

import com.demo.event.model.dto.request.CreateRelativeRequest;
import com.demo.event.model.dto.response.*;

import com.demo.event.model.entity.Event;
import com.demo.event.model.entity.Relative;
import com.demo.event.model.entity.User;
import com.demo.event.repository.EventRepository;
import com.demo.event.repository.RelativeRepository;
import com.demo.event.repository.UserRepository;
import com.demo.event.exception.ForbiddenException;
import com.demo.event.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelativeService {

    private final RelativeRepository relativeRepo;
    private final UserRepository     userRepo;
    private final EventRepository eventRepo;
    private final ObjectMapper objectMapper;

    // ── GET LIST (có filter) ─────────────────────────────────────────────
    public List<RelativeResponse> getRelatives(
            Long userId, String groupTypeStr, String search) {
        Relative.GroupType groupType = (groupTypeStr != null && !groupTypeStr.isBlank())
                ? Relative.GroupType.valueOf(groupTypeStr) : null;

        return relativeRepo.findByFilters(userId, groupType, search)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── GROUP SUMMARY ────────────────────────────────────────────────────
    public List<GroupSummaryResponse> getGroupSummary(Long userId) {
        Map<String, String> labels = Map.of(
                "GIA_DINH", "Gia dinh",
                "VO_CHONG", "Vo/Chong",
                "CON_CAI",  "Con cai",
                "BAN_BE",   "Ban be"
        );
        return relativeRepo.countByGroupType(userId).stream()
                .map(row -> GroupSummaryResponse.builder()
                        .groupType(row[0].toString())
                        .displayName(labels.getOrDefault(row[0].toString(), row[0].toString()))
                        .count(((Long) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ── GET DETAIL ───────────────────────────────────────────────────────
    public RelativeDetailResponse getDetail(Long id, Long userId) {
        Relative r = findByIdAndOwner(id, userId);

        int age = (r.getDateOfBirth() != null)
                ? Period.between(r.getDateOfBirth(), LocalDate.now()).getYears()
                : 0;

        List<Event> events =
                eventRepo.findByRelativeIdAndIsActiveTrueOrderByEventDateAsc(id);

        return RelativeDetailResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .nickname(r.getNickname())
                .groupType(r.getGroupType().name())
                .gender(r.getGender() != null ? r.getGender().name() : null)
                .age(age)
                .dateOfBirth(r.getDateOfBirth())
                .location(r.getLocation())
                .heightCm(r.getHeightCm())
                .weightKg(r.getWeightKg())
                .hobbies(parseHobbies(r.getHobbies()))
                .avatarUrl(r.getAvatarUrl())
                .daysUntilBirthday(calcDaysToBirthday(r.getDateOfBirth()))
                .relatedEvents(events.stream()
                        .map(e -> toEventResponse(e, LocalDate.now()))
                        .collect(Collectors.toList()))
                .build();
    }

    // ── CREATE ───────────────────────────────────────────────────────────
    @Transactional
    public RelativeResponse create(Long userId, CreateRelativeRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nguoi dung khong ton tai"));

        Relative r = Relative.builder()
                .user(user)
                .name(req.getName())
                .nickname(req.getNickname())
                .groupType(Relative.GroupType.valueOf(req.getGroupType()))
                .gender(req.getGender() != null
                        ? Relative.Gender.valueOf(req.getGender()) : null)
                .dateOfBirth(req.getDateOfBirth())
                .location(req.getLocation())
                .heightCm(req.getHeightCm())
                .weightKg(req.getWeightKg())
                .hobbies(req.getHobbies())
                .avatarUrl(req.getAvatarUrl())
                .totalEvents(0)
                .build();

        Relative saved = relativeRepo.save(r);
        userRepo.incrementRelativeCount(userId);
        return toResponse(saved);
    }

    // ── UPDATE ───────────────────────────────────────────────────────────
    @Transactional
    public RelativeResponse update(Long id, Long userId, CreateRelativeRequest req) {
        Relative r = findByIdAndOwner(id, userId);

        r.setName(req.getName());
        r.setNickname(req.getNickname());
        r.setGroupType(Relative.GroupType.valueOf(req.getGroupType()));
        r.setGender(req.getGender() != null
                ? Relative.Gender.valueOf(req.getGender()) : null);
        r.setDateOfBirth(req.getDateOfBirth());
        r.setLocation(req.getLocation());
        r.setHeightCm(req.getHeightCm());
        r.setWeightKg(req.getWeightKg());
        r.setHobbies(req.getHobbies());
        if (req.getAvatarUrl() != null) r.setAvatarUrl(req.getAvatarUrl());

        return toResponse(relativeRepo.save(r));
    }

    // ── DELETE ───────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id, Long userId) {
        Relative r = findByIdAndOwner(id, userId);
        relativeRepo.delete(r);
        userRepo.decrementRelativeCount(userId);
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────────────
    /** Tìm Relative và kiểm tra ownership. */
    private Relative findByIdAndOwner(Long id, Long userId) {
        Relative r = relativeRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Nguoi than khong ton tai: " + id));
        if (!r.getUser().getId().equals(userId))
            throw new ForbiddenException("Ban khong co quyen truy cap nguoi than nay");
        return r;
    }

    /** Tính số ngày đến sinh nhật tiếp theo (0 = hôm nay, -1 = không có ngày sinh). */
    private Integer calcDaysToBirthday(LocalDate dob) {
        if (dob == null) return -1;
        LocalDate today = LocalDate.now();
        LocalDate next  = dob.withYear(today.getYear());
        if (!next.isAfter(today)) next = next.plusYears(1);
        return (int) ChronoUnit.DAYS.between(today, next);
    }

    /** Parse chuỗi JSON hobbies thành List<String>. */
    private List<String> parseHobbies(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Map Relative -> RelativeResponse (dùng cho list). */
    private RelativeResponse toResponse(Relative r) {
        // Tìm sự kiện gần nhất của người thân
        List<Event> events =
                eventRepo.findByRelativeIdAndIsActiveTrueOrderByEventDateAsc(r.getId());
        String nextTitle = events.isEmpty() ? null : events.get(0).getTitle();

        return RelativeResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .nickname(r.getNickname())
                .groupType(r.getGroupType().name())
                .gender(r.getGender() != null ? r.getGender().name() : null)
                .dateOfBirth(r.getDateOfBirth())
                .location(r.getLocation())
                .avatarUrl(r.getAvatarUrl())
                .totalEvents(r.getTotalEvents())
                .daysUntilBirthday(calcDaysToBirthday(r.getDateOfBirth()))
                .nextEventTitle(nextTitle)
                .build();
    }

    /** Map Event -> EventResponse (dùng trong detail). */
    private EventResponse toEventResponse(Event e, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, e.getEventDate());
        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .eventType(e.getEventType().name())
                .eventDate(e.getEventDate())
                .eventTime(e.getEventTime())
                .daysUntil(days)
                .build();
    }
}
