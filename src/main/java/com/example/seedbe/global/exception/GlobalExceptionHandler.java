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
        // 유효성 검사 에러가 발생한 첫 번째 필드명과 원인만 추출 (입력값은 로그에 남기지 않음)
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
        log.error("Unhandled Exception 발생 : ", e);

        ErrorType errorType = ErrorType.INTERNAL_SERVER_ERROR;

        ApiResponse<Void> response = ApiResponse.error(errorType.getCode(), errorType.getMessage());

        return new ResponseEntity<>(response, errorType.getHttpStatus());
    }
}
