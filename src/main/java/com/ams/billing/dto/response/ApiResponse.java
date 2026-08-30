package com.ams.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ErrorDetail error;

    @Builder.Default
    private final String timestamp = Instant.now().toString();

    private final String requestId;

    // ── convenience factories ─────────────────────────────
    public static <T> ApiResponse<T> ok(String message, T data, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String code, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetail.builder().code(code).build())
                .requestId(requestId)
                .build();
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private final String code;
        private final Object details;
    }
}