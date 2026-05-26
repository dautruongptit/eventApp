package com.demo.event.controller;

import com.demo.event.model.dto.request.CreateEventRequest;
import com.demo.event.model.dto.response.BaseResponse;
import com.demo.event.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Quan ly su kien ca nhan va su kien nguoi than")
public class EventController {

    private final EventService eventService;

    /**
     * GET /api/v1/events
     * Query params: type, relativeId, month (1-12), year (e.g. 2026)
     * Dùng cho màn hình "Sự kiện" — nhóm theo tháng ở phía client.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long relativeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(BaseResponse.success(
                eventService.getEvents(userId, type, relativeId, month, year)));
    }

    /**
     * GET /api/v1/events/upcoming?limit=5
     * Dùng cho các card sự kiện trên màn hình Home.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<BaseResponse<?>> getUpcoming(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(BaseResponse.success(
                eventService.getUpcoming(userId, limit)));
    }

    /**
     * GET /api/v1/events/{id}
     * Chi tiết một sự kiện bao gồm danh sách reminder.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(BaseResponse.success(
                eventService.getById(id, userId)));
    }

    /**
     * POST /api/v1/events
     * Tạo sự kiện mới.
     * Nếu relativeId = null → sự kiện bản thân.
     * Nếu relativeId có giá trị → sự kiện người thân.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<?>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(eventService.create(userId, req)));
    }

    /**
     * PUT /api/v1/events/{id}
     * Cập nhật sự kiện. Reminders cũ bị xoá và tạo lại.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.ok(BaseResponse.success(
                eventService.update(id, userId, req)));
    }

    /**
     * DELETE /api/v1/events/{id}
     * Soft delete: đặt isActive = false, giảm cache counter.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        eventService.delete(id, userId);
        return ResponseEntity.ok(BaseResponse.success("Xoa su kien thanh cong"));
    }
}
