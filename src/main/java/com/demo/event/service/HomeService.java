package com.demo.event.service;

import com.demo.event.exception.ResourceNotFoundException;
import com.demo.event.model.dto.response.EventResponse;
import com.demo.event.model.dto.response.HomeResponse;
import com.demo.event.model.dto.response.RelativeResponse;
import com.demo.event.model.entity.Event;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final EventRepository eventRepo;
    private final RelativeRepository relativeRepo;
    private final UserRepository userRepo;
    private final EventService   eventService;

    // ── HOME DASHBOARD ───────────────────────────────────────────────────
    public HomeResponse getHomeData(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nguoi dung khong ton tai"));
        LocalDate today = LocalDate.now();

        // 5 sự kiện sắp tới (cả người thân + bản thân)
        List<Event> upcomingRaw = eventRepo.findUpcoming(
                userId, today, today.plusDays(90), PageRequest.of(0, 5));

        // Tab "Sự kiện của tôi" – chỉ sự kiện bản thân
        List<Event> myEventsRaw = eventRepo.findMyUpcoming(
                userId, today, PageRequest.of(0, 10));

        // Danh sách người thân (tất cả, không filter)
        List<Relative> relatives = relativeRepo.findByFilters(userId, null, null);

        return HomeResponse.builder()
                .userName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .upcomingEvents(upcomingRaw.stream()
                        .map(e -> eventService.toResponse(e, today))
                        .collect(Collectors.toList()))
                .myEvents(myEventsRaw.stream()
                        .map(e -> eventService.toResponse(e, today))
                        .collect(Collectors.toList()))
                .relatives(relatives.stream()
                        .map(r -> toRelativeResponse(r, today))
                        .collect(Collectors.toList()))
                .googleCalendarConnected(user.getGoogleCalendarToken() != null)
                .build();
    }

    // ── MY EVENTS TAB ────────────────────────────────────────────────────
    public List<EventResponse> getMyEvents(Long userId) {
        LocalDate today = LocalDate.now();
        return eventRepo
                .findMyUpcoming(userId, today, PageRequest.of(0, 20))
                .stream()
                .map(e -> eventService.toResponse(e, today))
                .collect(Collectors.toList());
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────────────
    private RelativeResponse toRelativeResponse(Relative r, LocalDate today) {
        // Sự kiện gần nhất của người thân
        List<Event> events =
                eventRepo.findByRelativeIdAndIsActiveTrueOrderByEventDateAsc(r.getId());

        String nextTitle   = events.isEmpty() ? null : events.get(0).getTitle();
        Integer daysUntilBirthday = calcDaysToBirthday(r.getDateOfBirth());

        return RelativeResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .nickname(r.getNickname())
                .groupType(r.getGroupType().name())
                .avatarUrl(r.getAvatarUrl())
                .totalEvents(r.getTotalEvents())
                .daysUntilBirthday(daysUntilBirthday)
                .nextEventTitle(nextTitle)
                .build();
    }

    private Integer calcDaysToBirthday(java.time.LocalDate dob) {
        if (dob == null) return -1;
        LocalDate today = LocalDate.now();
        LocalDate next  = dob.withYear(today.getYear());
        if (!next.isAfter(today)) next = next.plusYears(1);
        return (int) ChronoUnit.DAYS.between(today, next);
    }
}
