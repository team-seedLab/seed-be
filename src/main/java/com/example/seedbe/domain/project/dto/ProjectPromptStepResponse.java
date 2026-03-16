package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.ProjectStepLog;

public record ProjectPromptStepResponse(
        String stepCode,       // 예: "draft_generation"
        String stepName,       // 예: "초안 생성"
        String providedPromptSnapshot,
        String formatPrompt
) {
    public static ProjectPromptStepResponse from(ProjectStepLog projectStepLog) {
        return new ProjectPromptStepResponse(
                projectStepLog.getRoadmapStep().getStepCode(),
                projectStepLog.getRoadmapStep().getDescription(),
                projectStepLog.getProvidedPromptSnapshot(),
                projectStepLog.getPromptTemplate().getFormatPrompt()
        );
    }
}
