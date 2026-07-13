package com.example.seedbe.domain.result.dto;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.result.entity.ProjectStepResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectStepResultResponse(
        UUID stepId,
        String stepCode,
        String stepName,
        String contentMarkdown,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectStepResultResponse of(ProjectStep step, ProjectStepResult result) {
        return new ProjectStepResultResponse(step.getStepId(), step.getRoadmapStep().getStepCode(),
                step.getRoadmapStep().getDescription(), result.getContentMarkdown(),
                result.getCreatedAt(), result.getUpdatedAt());
    }
}
