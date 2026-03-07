package com.example.seedbe.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorType errorType;

    public BusinessException(ErrorType errorType) {
        super(errorType.getMessage()); // 디버깅 시 로그에 찍히도록 부모에게 메시지 전달
        this.errorType = errorType;
    }
}
