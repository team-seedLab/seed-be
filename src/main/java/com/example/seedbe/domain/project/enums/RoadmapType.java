package com.example.seedbe.domain.project.enums;

import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum RoadmapType {
    REPORT("리포트형", "report", List.of(
            RoadmapStep.CONSTRAINT_ANALYSIS,
            RoadmapStep.ARGUMENT_STRUCTURING,
            RoadmapStep.DRAFT_GENERATION,
            RoadmapStep.REPORT_REVISION
    )),
    PAPER("논문형", "paper", List.of(
            RoadmapStep.PLANNING,
            RoadmapStep.DRAFTING,
            RoadmapStep.PAPER_REVISION,
            RoadmapStep.SUBMISSION
    )),
    PRESENTATION("발표형", "presentation", List.of(
            RoadmapStep.MESSAGE_EXTRACTION,
            RoadmapStep.STORYLINE,
            RoadmapStep.SLIDE_DESIGN,
            RoadmapStep.SCRIPT_GENERATION
    )),
    EXPERIMENT("실험/실습형", "experiment", List.of(
            RoadmapStep.CONCEPT_DEFINITION,
            RoadmapStep.DESIGN_METHOD,
            RoadmapStep.IMPLEMENTATION,
            RoadmapStep.EVALUATION
    )),
    STUDY_SUMMARY("공부요약형", "study_summary", List.of(
            RoadmapStep.MATERIAL_ANALYSIS,
            RoadmapStep.KNOWLEDGE_STRUCTURING,
            RoadmapStep.SUMMARY_GENERATION
    )),
    STUDY_LEARNING("개념학습형", "study_learning", List.of(
            RoadmapStep.CONCEPT_DEFINITION,
            RoadmapStep.KNOWLEDGE_CONNECTION,
            RoadmapStep.QUIZ_GENERATION
    ));

    private final String description;
    private final String typeCode;
    private final List<RoadmapStep> validSteps;

    public void validateStep(RoadmapStep step) {
        if (!this.validSteps.contains(step)) {
            throw new BusinessException(ErrorType.NO_MATCHING_ROADMAP_TYPE);
        }
    }
}