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
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A002", "로그인이 필요합니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "A003", "해당 리소스에 접근할 권한이 없습니다."),

    // [User] 유저
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "유저가 없습니다"),

    // [Project] 과제 관련
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 프로젝트를 찾을 수 없습니다."),
    PDF_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P002", "PDF 과제 파일 파싱에 실패했습니다."),
    MAX_FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "P003", "파일 개수가 초과되었습니다"),
    NO_CONTENT_TO_ANALYZE(HttpStatus.NOT_FOUND, "P004", "분석할 과제가 없습니다"),
    AI_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "P005", "AI 서버 오류가 발생했습니다."),
    AI_RESPONSE_PARSE_ERROR(HttpStatus.BAD_REQUEST, "P006", "AI 응답 파싱에 실패했습니다"),
    INVALID_ROADMAP_STEP(HttpStatus.BAD_REQUEST, "P007", "해당 로드맵 단계가 존재하지 않습니다"),
    NO_MATCHING_ROADMAP_TYPE(HttpStatus.BAD_REQUEST, "P008", "해당 로드맵 유형에서 지원하지 않는 단계입니다."),

    // [AI Prompt] AI 프롬프트 네비게이션 관련
    PROMPT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AP001", "단계별 프롬프트 생성에 실패했습니다."),
    PROMPT_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "AP002", "템플릿이 DB에 존재하지 않습니다"),
    STEP_SEQUENCE_VIOLATION(HttpStatus.BAD_REQUEST, "AP003", "이전 단계의 AI 결과를 먼저 입력해야 합니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
