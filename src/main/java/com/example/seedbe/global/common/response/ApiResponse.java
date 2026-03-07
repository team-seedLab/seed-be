package com.example.seedbe.global.common.response;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static com.example.seedbe.global.util.DateTimeUtil.GLOBAL_DATE_TIME_FORMATTER;

@Getter
public class ApiResponse<T> {
    private final T data;
    private final ApiStatus status;
    private final String errorCode;
    private final String errorMessage;
    private final String serverDataTime;

    private ApiResponse(T data, ApiStatus status, String errorCode, String errorMessage) {
        this.data = data;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.serverDataTime = LocalDateTime.now(ZoneOffset.UTC).format(GLOBAL_DATE_TIME_FORMATTER);
    }

    // 성공시 호출
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, ApiStatus.SUCCESS, null, null);
    }

    // 비즈니스 로직 실패시 호출(예측 가능한 에러)
    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(null, ApiStatus.FAIL, errorCode, errorMessage);
    }

    // 서버 내부 에러(알 수 없는 에러)
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        return new ApiResponse<>(null, ApiStatus.ERROR, errorCode, errorMessage);
    }
}
