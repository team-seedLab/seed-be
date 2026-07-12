package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectStepSummaryResponse(
        UUID stepId,
        RoadmapStep roadmapStep,
        int stepOrder,
        ProjectStepStatus status,
        LocalDateTime completedAt
) {
    public static ProjectStepSummaryResponse from(ProjectStep step) {
        return new ProjectStepSummaryResponse(step.getStepId(), step.getRoadmapStep(), step.getStepOrder(),
                step.getStatus(), step.getCompletedAt());
    }
}
