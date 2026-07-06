package com.demo.event.controller;

import com.demo.event.model.dto.request.CreateRelativeRequest;
import com.demo.event.model.dto.response.BaseResponse;
import com.demo.event.service.RelativeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/relatives")
@RequiredArgsConstructor
@Tag(name = "Relatives", description = "Quan ly nguoi than theo nhom")
public class RelativeController {

    private final RelativeService relativeService;

    /**
     * GET /api/v1/relatives
     * Query params: group_type (GIA_DINH|VO_CHONG|CON_CAI|BAN_BE), search (tên)
     */
    @GetMapping
    @Operation(
            summary = "Lay danh sach nguoi than",
            description = "Ho tro filter theo nhom quan he va tim kiem theo ten.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thanh cong"),
            @ApiResponse(responseCode = "401", description = "Token het han hoac khong hop le")
    })
    public ResponseEntity<BaseResponse<?>> getAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String groupType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(BaseResponse.success(
                relativeService.getRelatives(userId, groupType, search)));
    }

    /**
     * GET /api/v1/relatives/groups
     * Trả về tổng hợp số lượng người thân theo nhóm.
     * VD: [{groupType:"GIA_DINH", displayName:"Gia dinh", count:2}, ...]
     */
    @GetMapping("/groups")
    public ResponseEntity<BaseResponse<?>> getGroups(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(BaseResponse.success(
                relativeService.getGroupSummary(userId)));
    }

    /**
     * GET /api/v1/relatives/{id}
     * Chi tiết người thân + danh sách sự kiện liên quan.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(BaseResponse.success(
                relativeService.getDetail(id, userId)));
    }

    /**
     * POST /api/v1/relatives
     * Thêm người thân mới. 201 Created khi thành công.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<?>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRelativeRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(relativeService.create(userId, req)));
    }

    /**
     * PUT /api/v1/relatives/{id}
     * Cập nhật thông tin người thân. Trả 403 nếu không phải chủ sở hữu.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRelativeRequest req) {
        return ResponseEntity.ok(BaseResponse.success(
                relativeService.update(id, userId, req)));
    }

    /**
     * DELETE /api/v1/relatives/{id}
     * Xoá người thân và cascade xoá toàn bộ sự kiện liên quan.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<?>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        relativeService.delete(id, userId);
        return ResponseEntity.ok(BaseResponse.success("Xoa nguoi than thanh cong"));
    }
}
