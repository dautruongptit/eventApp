package com.demo.event.service;

import com.demo.event.exception.CustomException;
import com.demo.event.model.dto.request.CreateRelativeRequest;
import com.demo.event.model.dto.response.GroupSummaryResponse;
import com.demo.event.model.dto.response.RelativeDetailResponse;
import com.demo.event.model.dto.response.RelativeResponse;
import com.demo.event.model.entity.Relative;
import com.demo.event.model.entity.User;
import com.demo.event.repository.RelativeRepository;
import com.demo.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

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
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper; // Jackson, parse hobbies JSON

    public List<RelativeResponse> getRelatives(Long userId,
                                               String groupTypeStr, String search) {
        Relative.GroupType groupType = groupTypeStr != null
                ? Relative.GroupType.valueOf(groupTypeStr) : null;
        return relativeRepo.findByFilters(userId, groupType, search)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<GroupSummaryResponse> getGroupSummary(Long userId) {
        Map<String, String> labels = Map.of(
                "GIA_DINH", "Gia đình",
                "VO_CHONG", "Vợ/Chồng",
                "CON_CAI", "Con cái",
                "BAN_BE", "Bạn bè"
        );
        return relativeRepo.countByGroupType(userId).stream()
                .map(row -> GroupSummaryResponse.builder()
                        .groupType(row[0].toString())
                        .displayName(labels.get(row[0].toString()))
                        .count(((Long) row[1]).intValue())
                        .build()
                ).collect(Collectors.toList());
    }

    public RelativeDetailResponse getDetail(Long id, Long userId) {
        Relative r = relativeRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("Người thân không tồn tại"));
        int age = (r.getDateOfBirth() != null)
                ? Period.between(r.getDateOfBirth(), LocalDate.now()).getYears() : 0;
        return RelativeDetailResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .groupType(r.getGroupType().name())
                .age(age)
                .dateOfBirth(r.getDateOfBirth())
                .location(r.getLocation())
                .heightCm(r.getHeightCm())
                .weightKg(r.getWeightKg())
                .hobbies(parseHobbies(r.getHobbies()))
                .daysUntilBirthday(calcDaysToBirthday(r.getDateOfBirth()))
                .relatedEvents(r.getEvents().stream()
                        .map(this::toEventResponse).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public RelativeResponse create(Long userId, CreateRelativeRequest req) {
        User user = userRepo.findById(userId).orElseThrow();
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
                .build();
        Relative saved = relativeRepo.save(r);
        userRepo.incrementRelativeCount(userId);
        return toResponse(saved);
    }

    // Tính số ngày đến sinh nhật (tính theo năm hiện tại/năm sau)
    private Integer calcDaysToBirthday(LocalDate dob) {
        if (dob == null) return -1;
        LocalDate today = LocalDate.now();
        LocalDate next = dob.withYear(today.getYear());
        if (next.isBefore(today)) next = next.plusYears(1);
    }

    private List<String> parseHobbies(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<String>>() {});
        } catch (Exception e) { return List.of(); }
    }
}
