package com.demo.event.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body cho POST /auth/google.
 * Client (Google Sign-In SDK) gui idToken lay tu Google, backend se verify.
 */
@Data
public class GoogleLoginRequest {

    @NotBlank(message = "idToken khong duoc de trong")
    private String idToken;
}
