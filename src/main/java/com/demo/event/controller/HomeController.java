package com.demo.event.controller;

import com.demo.event.model.dto.response.BaseResponse;
import com.demo.event.service.HomeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home Dashboard", description = "Du lieu tong hop man hinh Home")
public class HomeController {

    private final HomeService homeService;

    /**
     * GET /api/v1/home
     * Trả về toàn bộ dữ liệu cần cho màn hình Home:
     * - userName, avatarUrl
     * - upcomingEvents (tối đa 5, cả người thân + bản thân)
     * - myEvents (tab "Sự kiện của tôi")
     * - relatives (danh sách + sự kiện gần nhất của mỗi người)
     * - googleCalendarConnected
     */
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getHomeData(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
                BaseResponse.success(homeService.getHomeData(userId)));
    }

    /**
     * GET /api/v1/home/my-events
     * Tab "Sự kiện của tôi" — chỉ lấy sự kiện bản thân (relative IS NULL).
     */
    @GetMapping("/my-events")
    public ResponseEntity<BaseResponse<?>> getMyEvents(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
                BaseResponse.success(homeService.getMyEvents(userId)));
    }
}
