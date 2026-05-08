package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.ProjectStepLog;

import java.time.LocalDateTime;

public record ProjectPromptStepResponse(
        String stepCode,
        String stepName,
        String providedPromptSnapshot,
        String formatPrompt,
        String userSubmittedResult,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectPromptStepResponse from(ProjectStepLog projectStepLog) {
        return new ProjectPromptStepResponse(
                projectStepLog.getRoadmapStep().getStepCode(),
                projectStepLog.getRoadmapStep().getDescription(),
                projectStepLog.getProvidedPromptSnapshot(),
                projectStepLog.getPromptTemplate().getFormatPrompt(),
                projectStepLog.getUserSubmittedResult(),
                projectStepLog.getCreatedAt(),
                projectStepLog.getUpdatedAt()
        );
    }
}
