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
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "A001", "이메일 또는 비밀번호가 올바르지 않습니다."),

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
    NOT_LAST_STEP(HttpStatus.BAD_REQUEST, "P009", "마지막 단계가 아닙니다"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "P010", "지원하지 않는 파일 형식입니다. PDF만 업로드할 수 있습니다."),
    EMPTY_PDF_TEXT(HttpStatus.BAD_REQUEST, "P011", "PDF에서 분석 가능한 텍스트를 추출하지 못했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "P012", "PDF 파일은 개당 20MB를 초과할 수 없습니다."),
    INVALID_ROADMAP_TEMPLATE(HttpStatus.INTERNAL_SERVER_ERROR, "P013", "로드맵 단계와 프롬프트 템플릿 구성이 올바르지 않습니다."),


    // [AI Prompt] AI 프롬프트 네비게이션 관련
    PROMPT_TEMPLATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "AP001", "프롬프트 템플릿이 DB에 존재하지 않습니다"),
    STEP_NOT_STARTED(HttpStatus.NOT_FOUND, "AP002", "필요 단계가 아직 시작되지 않았습니다. 프롬프트를 먼저 발급받으세요."),
    GENERATED_RESULT_NOT_FOUND(HttpStatus.BAD_REQUEST, "AP003", "해당 단계 결과를 등록하셔야 합니다"),
    GENERATED_PROMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "AP004", "해당 단계 프롬프트를 찾을 수 없습니다"),

    // [AI Mentor] 단계별 AI 멘토 관련
    AI_MENTOR_QUESTION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "AM001", "해당 단계의 AI 멘토 질문 횟수를 초과했습니다."),
    EDITED_PROMPT_NOT_FOUND(HttpStatus.BAD_REQUEST, "AM002", "수정한 프롬프트가 없어 다시 묻기를 실행할 수 없습니다."),
    AI_MENTOR_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AM003", "AI 멘토 응답을 생성하지 못했습니다."),
    AI_MENTOR_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "AM004", "AI 멘토 응답을 처리하지 못했습니다."),
    AI_MENTOR_RESPONSE_TRUNCATED(HttpStatus.BAD_GATEWAY, "AM005", "AI 멘토 답변 생성이 완료되지 않았습니다. 다시 시도해 주세요."),
    AI_MENTOR_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AM006", "오늘 사용할 수 있는 AI 멘토 질문 40회를 모두 사용했습니다."),
    AI_MENTOR_RESPONSE_FORMAT_ERROR(HttpStatus.BAD_GATEWAY, "AM007", "AI 멘토가 답변 형식을 지키지 못했습니다. 다시 시도해 주세요.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
