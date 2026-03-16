package com.example.seedbe.global.common.constants;

public final class SwaggerConstants {

    private SwaggerConstants() {
        // 인스턴스화 방지
    }

    // 텍스트 블록(Text Block)을 사용하여 가독성을 높였습니다.
    public static final String STEP_CODE_DESCRIPTION = """
            ### 📍 로드맵 타입별 단계 코드 가이드
            프로젝트 타입에 맞는 코드를 입력창에 복사해서 사용하세요.

            | 로드맵 타입 | 단계 코드 (복사해서 입력) | 단계명 |
            | :--- | :--- | :--- |
            | **REPORT (리포트형)** | `constraint_analysis` | 제약사항 분석 |
            | | `argument_structuring` | 논거 구조화 |
            | | `draft_generation` | 초안 생성 |
            | | `report_revision` | 교정 및 검토 |
            | :--- | :--- | :--- |
            | **PAPER (논문형)** | `planning` | 연구 계획 |
            | | `drafting` | 초고 작성 |
            | | `paper_revision` | 교정 및 검토 |
            | | `submission` | 투고 준비 |
            | :--- | :--- | :--- |
            | **PRESENTATION (발표형)** | `message_extraction` | 메세지 추출 |
            | | `storyline` | 스토리라인 작성 |
            | | `slide_design` | 슬라이드 디자인 |
            | | `script_generation` | 스크립트 생성 |
            | :--- | :--- | :--- |
            | **EXPERIMENT (실험형)** | `requirement_definition` | 요구사항 정의 |
            | | `design_method` | 실험 설계 |
            | | `implementation` | 실험 진행 |
            | | `evaluation` | 결과 평가 |
            | :--- | :--- | :--- |
            | **STUDY_SUMMARY (요약형)** | `material_analysis` | 자료 분석 |
            | | `knowledge_structuring` | 지식 구조화 |
            | | `summary_generation` | 요약본 생성 |
            | :--- | :--- | :--- |
            | **STUDY_LEARNING (학습형)** | `concept_definition` | 개념 정의 |
            | | `knowledge_connection` | 지식 연결 |
            | | `quiz_generation` | 퀴즈 생성 |
            """;
}