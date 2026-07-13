package com.example.seedbe.domain.prompt.component;

import com.example.seedbe.domain.project.enums.RoadmapStep;
import org.springframework.stereotype.Component;

@Component
public class StepPromptComposer {
    public String compose(RoadmapStep step) {
        return """
                저장된 과제 자료와 사용자 요구사항을 참고하여 '%s' 단계를 진행해 주세요.

                %s
                자료에서 확인할 수 없는 내용은 임의로 확정하지 말고 추가 확인 항목으로 구분해 주세요.
                """.formatted(step.getDescription(), objective(step)).trim();
    }

    private String objective(RoadmapStep step) {
        return switch (step) {
            case CONSTRAINT_ANALYSIS -> "제출 조건, 평가 기준, 필수 포함 요소와 제한사항을 구분해 정리하세요.";
            case ARGUMENT_STRUCTURING -> "핵심 주장과 이를 뒷받침하는 근거의 연결 구조를 설계하세요.";
            case DRAFT_GENERATION, DRAFTING -> "앞 단계에서 정리한 구조를 바탕으로 논리적인 초안을 작성하세요.";
            case REPORT_REVISION, PAPER_REVISION -> "초안의 논리, 표현, 누락 사항과 요구사항 충족 여부를 점검하세요.";
            case PLANNING -> "연구 목적, 핵심 질문, 범위와 진행 계획을 구체화하세요.";
            case SUBMISSION -> "제출 형식, 인용, 구성과 최종 요구사항 충족 여부를 확인하세요.";
            case MESSAGE_EXTRACTION -> "발표에서 전달해야 할 핵심 메시지와 근거를 추출하세요.";
            case STORYLINE -> "핵심 메시지가 자연스럽게 이어지도록 발표 흐름을 구성하세요.";
            case SLIDE_DESIGN -> "슬라이드별 목적과 핵심 내용을 구분하여 화면 구성을 설계하세요.";
            case SCRIPT_GENERATION -> "발표 시간과 청중을 고려하여 슬라이드별 발표 대본을 작성하세요.";
            case REQUIREMENT_DEFINITION -> "해결할 문제, 목표, 필수 조건과 성공 기준을 정의하세요.";
            case DESIGN_METHOD -> "실험 또는 구현 절차, 변수, 도구와 검증 방법을 설계하세요.";
            case IMPLEMENTATION -> "설계한 절차에 따라 수행 과정과 관찰 결과를 기록하세요.";
            case EVALUATION -> "결과를 기준과 비교하고 오차, 한계와 개선 방향을 분석하세요.";
            case MATERIAL_ANALYSIS -> "자료의 범위, 핵심 주제와 중요도를 분석하세요.";
            case KNOWLEDGE_STRUCTURING -> "핵심 개념과 세부 내용을 관계 중심으로 구조화하세요.";
            case SUMMARY_GENERATION -> "구조화한 내용을 학습 목적에 맞는 요약본으로 작성하세요.";
            case CONCEPT_DEFINITION -> "핵심 개념의 정의, 특징과 적용 범위를 명확히 정리하세요.";
            case KNOWLEDGE_CONNECTION -> "핵심 개념과 관련 개념의 공통점, 차이점과 연결 관계를 설명하세요.";
            case QUIZ_GENERATION -> "학습 내용을 점검할 수 있도록 난이도별 문제와 해설을 구성하세요.";
        };
    }
}
