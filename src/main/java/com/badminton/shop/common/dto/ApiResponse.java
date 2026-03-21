package com.badminton.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final String message;
    private final String status;
    private final int statusCode;
    private final T data;

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return new ApiResponse<>(message, "success", status.value(), data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(HttpStatus.OK, message, data);
    }

    public static ApiResponse<Object> error(HttpStatus status, String message) {
        return new ApiResponse<>(message, "error", status.value(), null);
    }
}