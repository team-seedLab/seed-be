package com.example.seedbe.domain.project.enums;

import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum RoadmapType {

    // 각 로드맵 타입은 자신이 허용하는 단계(Step)들만 명시적으로 소유합니다.
    REPORT("리포트형", "report", Arrays.asList(
            RoadmapStep.CONSTRAINT_ANALYSIS,
            RoadmapStep.ARGUMENT_STRUCTURING,
            RoadmapStep.DRAFT_GENERATION,
            RoadmapStep.REPORT_REVISION
    )),
    PAPER("논문형", "paper", Arrays.asList(
            RoadmapStep.PLANNING,
            RoadmapStep.DRAFTING,
            RoadmapStep.PAPER_REVISION,
            RoadmapStep.SUBMISSION
    )),
    EXPERIMENT("실험/실습형", "experiment", Arrays.asList(
            RoadmapStep.CONCEPT_DEFINITION,
            RoadmapStep.DESIGN_METHOD,
            RoadmapStep.IMPLEMENTATION,
            RoadmapStep.EVALUATION
    )),
    STUDY_SUMMARY("공부요약형", "study_summary", Arrays.asList(
            RoadmapStep.MATERIAL_ANALYSIS,
            RoadmapStep.KNOWLEDGE_STRUCTURING,
            RoadmapStep.SUMMARY_GENERATION
    )),
    STUDY_LEARNING("개념학습형", "study_learning", Arrays.asList(
            RoadmapStep.CONCEPT_DEFINITION,
            RoadmapStep.KNOWLEDGE_CONNECTION,
            RoadmapStep.QUIZ_GENERATION
    ));

    private final String description;
    private final String taskCode;
    private final List<RoadmapStep> validSteps;

    public void validateStep(RoadmapStep step) {
        if (!this.validSteps.contains(step)) {
            throw new BusinessException(ErrorType.INVALID_ROADMAP_STEP);
        }
    }
}