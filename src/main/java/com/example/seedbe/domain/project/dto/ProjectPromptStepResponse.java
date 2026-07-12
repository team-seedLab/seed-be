package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.ProjectStep;

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
    public static ProjectPromptStepResponse from(ProjectStep projectStep) {
        return new ProjectPromptStepResponse(
                projectStep.getRoadmapStep().getStepCode(),
                projectStep.getRoadmapStep().getDescription(),
                projectStep.getPrompt().getProvidedPromptSnapshot(),
                projectStep.getPromptTemplate().getFormatPrompt(),
                projectStep.getResult() == null ? null : projectStep.getResult().getContentMarkdown(),
                projectStep.getPrompt().getCreatedAt(),
                projectStep.getPrompt().getUpdatedAt()
        );
    }
}
