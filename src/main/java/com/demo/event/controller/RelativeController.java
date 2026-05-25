package com.demo.event.controller;

import com.demo.event.model.dto.request.CreateRelativeRequest;
import com.demo.event.model.dto.response.ApiResponse;
import com.demo.event.service.RelativeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relatives")
@RequiredArgsConstructor
@Tag(name = "Relatives", description = "Quan ly nguoi than theo nhom")
public class RelativeController {

    private final RelativeService relativeService;

    /**
     * GET /api/v1/relatives
     * Query params: group_type (GIA_DINH|VO_CHONG|CON_CAI|BAN_BE), search (tên)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String groupType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                relativeService.getRelatives(userId, groupType, search)));
    }

    /**
     * GET /api/v1/relatives/groups
     * Trả về tổng hợp số lượng người thân theo nhóm.
     * VD: [{groupType:"GIA_DINH", displayName:"Gia dinh", count:2}, ...]
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<?>> getGroups(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                relativeService.getGroupSummary(userId)));
    }

    /**
     * GET /api/v1/relatives/{id}
     * Chi tiết người thân + danh sách sự kiện liên quan.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                relativeService.getDetail(id, userId)));
    }

    /**
     * POST /api/v1/relatives
     * Thêm người thân mới. 201 Created khi thành công.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRelativeRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(relativeService.create(userId, req)));
    }

    /**
     * PUT /api/v1/relatives/{id}
     * Cập nhật thông tin người thân. Trả 403 nếu không phải chủ sở hữu.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRelativeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                relativeService.update(id, userId, req)));
    }

    /**
     * DELETE /api/v1/relatives/{id}
     * Xoá người thân và cascade xoá toàn bộ sự kiện liên quan.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        relativeService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Xoa nguoi than thanh cong"));
    }
}
