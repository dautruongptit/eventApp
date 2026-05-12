package com.demo.event.service;

import com.demo.event.exception.ForbiddenException;
import com.demo.event.model.dto.response.NotificationResponse;
import com.demo.event.model.entity.Notification;
import com.demo.event.repository.NotificationRepository;
import com.demo.event.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notifRepo;

    // ── GET LIST (phân trang) ────────────────────────────────────────────
    public Page<NotificationResponse> getNotifications(
            Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notifRepo
                .findByUserIdOrderBySentAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    // ── COUNT UNREAD ─────────────────────────────────────────────────────
    public long countUnread(Long userId) {
        return notifRepo.countByUserIdAndIsReadFalse(userId);
    }

    // ── MARK ONE AS READ ─────────────────────────────────────────────────
    @Transactional
    public NotificationResponse markAsRead(Long id, Long userId) {
        Notification n = notifRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Thong bao khong ton tai: " + id));

        if (!n.getUser().getId().equals(userId))
            throw new ForbiddenException("Ban khong co quyen truy cap thong bao nay");

        n.setIsRead(true);
        return toResponse(notifRepo.save(n));
    }

    // ── MARK ALL AS READ ─────────────────────────────────────────────────
    @Transactional
    public void markAllAsRead(Long userId) {
        notifRepo.markAllAsRead(userId);
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────────────
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.getIsRead())
                .eventId(n.getEvent() != null ? n.getEvent().getId() : null)
                .eventTitle(n.getEvent() != null ? n.getEvent().getTitle() : null)
                .sentAt(n.getSentAt())
                .build();
    }
}
