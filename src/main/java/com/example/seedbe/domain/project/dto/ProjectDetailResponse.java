package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStepLog;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectDetailResponse(
        UUID projectId,
        String title,
        RoadmapType roadmapType,
        ProjectStatus status,
        LocalDateTime createdAt,
        List<ProjectPromptStepResponse> stepResponses
) {
    public static ProjectDetailResponse from(Project project) {
        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getRoadmapType(),
                project.getStatus(),
                project.getCreatedAt(),
                List.of()
        );
    }

    public static ProjectDetailResponse of(Project project, List<ProjectStepLog> stepLogs) {
        List<ProjectPromptStepResponse> stepResponses = stepLogs.stream()
                .map(ProjectPromptStepResponse::from)
                .toList();

        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getRoadmapType(),
                project.getStatus(),
                project.getCreatedAt(),
                stepResponses
        );
    }
}
