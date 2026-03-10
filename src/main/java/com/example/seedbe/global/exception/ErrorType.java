package com.example.seedbe.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorType {
    // [Global] 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G002", "서버 내부 오류가 발생했습니다."),

    // [Auth] 인증/인가
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "A001", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A002", "로그인이 필요합니다."),

    // [User] 유저
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "유저가 없습니다"),

    // [Project] 과제 PDF 관련
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 프로젝트를 찾을 수 없습니다."),
    PDF_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P002", "PDF 과제 파일 파싱에 실패했습니다."),

    // [AI Prompt] AI 프롬프트 네비게이션 관련
    PROMPT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A001", "단계별 프롬프트 생성에 실패했습니다."),
    STEP_SEQUENCE_VIOLATION(HttpStatus.BAD_REQUEST, "A002", "이전 단계의 AI 결과를 먼저 입력해야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
