package com.example.seedbe.global.exception;

import com.example.seedbe.global.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
     // 비즈니스 로직 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException 발생 : {}", e.getMessage());
        ErrorType errorType = e.getErrorType();

        ApiResponse<Void> response = ApiResponse.fail(errorType.getCode(), errorType.getMessage());

        return new ResponseEntity<>(response, errorType.getHttpStatus());
    }

    // 사용자 입력값 검증 예외 처리 (@Valid 실패 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 첫 번째 유효성 검사 에러 메시지만 추출
        String fieldName = e.getBindingResult().getFieldErrors().get(0).getField();
        String errorMessage = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        log.warn("ValidationException 발생 : [{}] {}", fieldName, errorMessage);

        ErrorType errorType = ErrorType.INVALID_INPUT_VALUE;

        ApiResponse<Void> response = ApiResponse.fail(errorType.getCode(), errorMessage);

        return new ResponseEntity<>(response, errorType.getHttpStatus());
    }

    // 처리하지 못한 모든 서버 내부 에러 (NullPointerException, DB 커넥션 끊김 등)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 알 수 없는 에러는 예외가 발생한 지점을 역으로 추적할 수 있도록 반드시 로그와 스택 트레이스를 남긴다
        log.error("Unhandled Exception 발생 : ", e);

        ErrorType errorType = ErrorType.INTERNAL_SERVER_ERROR;

        ApiResponse<Void> response = ApiResponse.error(errorType.getCode(), errorType.getMessage());

        return new ResponseEntity<>(response, errorType.getHttpStatus());
    }
}
