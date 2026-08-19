package com.risingbee.realestate.automation.dto;

public record ApiResponse<T>(
        boolean success,
        String message,
        String code,
        T data,
        Object error
) {
    public static <T> ApiResponse<T> success(String message, String code, T data) {
        return new ApiResponse<>(true, message, code, data, null);
    }

    public static <T> ApiResponse<T> error(String message, String code, Object error) {
        return new ApiResponse<>(false, message, code, null, error);
    }
}