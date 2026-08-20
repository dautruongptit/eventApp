package com.demo.event.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Cập nhật họ tên hồ sơ cá nhân (PUT /users/me).
 * Tách riêng khỏi RegisterRequest vì update không cần email/password.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "fullName khong duoc de trong")
    @Size(min = 2, max = 100)
    private String fullName;
}
