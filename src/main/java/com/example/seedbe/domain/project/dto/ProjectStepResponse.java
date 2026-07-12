package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectStepResponse(
        UUID stepId,
        RoadmapStep roadmapStep,
        int stepOrder,
        ProjectStepStatus status,
        LocalDateTime completedAt
) {
    public static ProjectStepResponse from(ProjectStep step) {
        return new ProjectStepResponse(step.getStepId(), step.getRoadmapStep(), step.getStepOrder(),
                step.getStatus(), step.getCompletedAt());
    }
}
