package com.demo.event.exception;

import com.demo.event.model.dto.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse<?>> handleBadRequest(
            BadRequestException ex) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(ex.getMessage()));
    }

    // 401 Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse<?>> handleUnauthorized(
            UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleNotFound(
            ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // 403 Forbidden (truy cập tài nguyên của user khác)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponse<?>> handleForbidden(
            ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // 422 Validation errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidation(
            MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(BaseResponse.error(msg));
    }

    // 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleAll(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(BaseResponse.error("Lỗi hệ thống: " + ex.getMessage()));
    }
}
