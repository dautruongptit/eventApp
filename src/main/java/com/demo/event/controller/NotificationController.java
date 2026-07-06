package com.demo.event.controller;

import com.demo.event.model.dto.response.BaseResponse;
import com.demo.event.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thong bao va trang thai da doc / chua doc")
public class NotificationController {

    private final NotificationService notifService;

    /**
     * GET /api/v1/notifications?page=0&size=20
     * Danh sách thông báo của user hiện tại, phân trang, mới nhất trước.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<?>> getAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<?> result = notifService.getNotifications(userId, page, size);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    /**
     * GET /api/v1/notifications/unread-count
     * Trả về số thông báo chưa đọc. Dùng để hiển thị badge.
     * VD: { "success": true, "data": 3 }
     */
    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponse<?>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
                BaseResponse.success(notifService.countUnread(userId)));
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     * Đánh dấu một thông báo cụ thể là đã đọc.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<BaseResponse<?>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(BaseResponse.success(
                notifService.markAsRead(id, userId)));
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Đánh dấu tất cả thông báo của user là đã đọc.
     */
    @PutMapping("/read-all")
    public ResponseEntity<BaseResponse<?>> markAllAsRead(
            @AuthenticationPrincipal Long userId) {
        notifService.markAllAsRead(userId);
        return ResponseEntity.ok(
                BaseResponse.success("Tat ca thong bao da duoc danh dau da doc"));
    }
}
