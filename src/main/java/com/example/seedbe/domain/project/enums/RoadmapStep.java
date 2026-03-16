package com.example.seedbe.domain.project.enums;

import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum RoadmapStep {
    // --- REPORT (리포트형) ---
    CONSTRAINT_ANALYSIS("constraint_analysis", "제약사항 분석"),
    ARGUMENT_STRUCTURING("argument_structuring", "논거 구조화"),
    DRAFT_GENERATION("draft_generation", "초안 생성"),
    REPORT_REVISION("report_revision", "교정 및 검토"),

    // --- PAPER (논문형) ---
    PLANNING("planning", "연구 계획"),
    DRAFTING("drafting", "초고 작성"),
    PAPER_REVISION("paper_revision", "교정 및 검토"), // 리포트형과 논문형 등에서 공통으로 사용 가능
    SUBMISSION("submission", "투고 준비"),

    // --- PRESENTATION (발표형) ---
    MESSAGE_EXTRACTION("message_extraction", "메세지 추출"),
    STORYLINE("storyline", "스토리라인 작성"),
    SLIDE_DESIGN("slide_design", "슬라이드 디자인"), // 리포트형과 논문형 등에서 공통으로 사용 가능
    SCRIPT_GENERATION("script_generation", "스크립트 생성"),

    // --- EXPERIMENT (실험/실습형) ---
    REQUIREMENT_DEFINITION("requirement_definition", "요구사항 정의"),
    DESIGN_METHOD("design_method", "실험 설계"), // CSV 데이터 기준 첫 단계
    IMPLEMENTATION("implementation", "실험 진행"),
    EVALUATION("evaluation", "결과 평가"),

    // --- STUDY_SUMMARY (자료요약형) ---
    MATERIAL_ANALYSIS("material_analysis", "자료 분석"),
    KNOWLEDGE_STRUCTURING("knowledge_structuring", "지식 구조화"),
    SUMMARY_GENERATION("summary_generation", "요약본 생성"),

    // --- STUDY_LEARNING (개념학습형) ---
    CONCEPT_DEFINITION("concept_definition", "개념 정의"),
    KNOWLEDGE_CONNECTION("knowledge_connection", "지식 연결"),
    QUIZ_GENERATION("quiz_generation", "퀴즈 생성");

    private final String stepCode;
    private final String description;

    public static RoadmapStep fromStepCode(String code) {
        return Arrays.stream(values())
                .filter(step -> step.getStepCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorType.INVALID_ROADMAP_STEP));
    }
}
