package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectListResponse(
        UUID projectId,
        String title,
        RoadmapType roadmapType,
        ProjectStatus projectStatus,
        RoadmapStep currentRoadmapStep,
        Integer currentStepOrder,
        int totalStepCount,
        int completedStepCount,
        int progressPercent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static ProjectListResponse of(Project project, RoadmapStep currentRoadmapStep,
                                         Integer currentStepOrder, int totalStepCount,
                                         int completedStepCount, int progressPercent) {
        return new ProjectListResponse(project.getProjectId(), project.getTitle(), project.getRoadmapType(),
                project.getStatus(), currentRoadmapStep, currentStepOrder, totalStepCount,
                completedStepCount, progressPercent, project.getCreatedAt(),
                project.getUpdatedAt(), project.getCompletedAt());
    }
}
