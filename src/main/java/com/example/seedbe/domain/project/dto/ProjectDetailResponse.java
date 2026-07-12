package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectDetailResponse(
        UUID projectId,
        String title,
        RoadmapType roadmapType,
        ProjectStatus projectStatus,
        String desiredOutcome,
        String keyFocus,
        String requiredElements,
        RoadmapStep currentRoadmapStep,
        Integer currentStepOrder,
        int totalStepCount,
        int completedStepCount,
        int progressPercent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        List<ProjectStepResponse> steps
) {
    public static ProjectDetailResponse of(Project project, RoadmapStep currentRoadmapStep,
                                           Integer currentStepOrder, int totalStepCount,
                                           int completedStepCount, int progressPercent,
                                           List<ProjectStepResponse> steps) {
        return new ProjectDetailResponse(project.getProjectId(), project.getTitle(), project.getRoadmapType(),
                project.getStatus(), project.getDesiredOutcome(), project.getKeyFocus(),
                project.getRequiredElements(), currentRoadmapStep, currentStepOrder, totalStepCount,
                completedStepCount, progressPercent, project.getCreatedAt(), project.getUpdatedAt(),
                project.getCompletedAt(), steps);
    }
}
