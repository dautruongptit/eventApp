package com.demo.event.service;

import com.demo.event.model.dto.response.EventResponse;
import com.demo.event.model.dto.response.HomeResponse;
import com.demo.event.model.dto.response.RelativeResponse;
import com.demo.event.repository.EventRepository;
import com.demo.event.repository.RelativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final EventRepository    eventRepo;
    private final RelativeRepository relativeRepo;
    private final EventService       eventService;

    // ── GET HOME DATA — cache 5 phút ──────────────────────────────────────────
    @Cacheable(value = "home", key = "#userId")
    public HomeResponse getHomeData(Long userId) {
        List<EventResponse> upcomingEvents = eventService.getUpcoming(userId, 5);
        List<EventResponse> myEvents       = getMyEvents(userId);
        List<RelativeResponse> relatives   = relativeRepo
            .findByFilters(userId, null, null)
            .stream()
            .map(RelativeResponse::from)
            .toList();

        return HomeResponse.builder()
            .upcomingEvents(upcomingEvents)
            .myEvents(myEvents)
            .relatives(relatives)
            .build();
    }

    // ── GET MY EVENTS — cache 5 phút ─────────────────────────────────────────
    @Cacheable(value = "upcoming", key = "#userId + '::myEvents'")
    public List<EventResponse> getMyEvents(Long userId) {
        return eventRepo.findMyUpcoming(userId, LocalDate.now(), PageRequest.of(0, 20))
            .stream()
            .map(e -> eventService.toResponse(e, LocalDate.now()))
            .toList();
    }

    @CacheEvict(value = "home", key = "#userId")
    public void evictHome(Long userId) {
        // trigger evict thủ công nếu cần từ Service khác
    }
}
