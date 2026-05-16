package com.demo.event.service;


import com.demo.event.model.dto.response.UserProfileResponse;
import com.demo.event.model.entity.User;
import com.demo.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepo;

    /**
     * [ADMIN] Lay danh sach tat ca user, phan trang, sap xep theo
     * createdAt giam dan (moi nhat truoc).
     */
    public Page<UserProfileResponse> getAllUsers(int page, int size) {
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return userRepo.findAll(pageable).map(this::toResponse);
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────
    private UserProfileResponse toResponse(User u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .language(u.getLanguage())
                .darkMode(u.getDarkMode())
                .totalEvents(u.getTotalEvents())
                .totalRelatives(u.getTotalRelatives())
                .googleCalendarConnected(u.getGoogleCalendarToken() != null)
                .createdAt(u.getCreatedAt())
                .build();
    }
}
