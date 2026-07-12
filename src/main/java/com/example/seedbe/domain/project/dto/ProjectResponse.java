package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID projectId,
        String title,
        RoadmapType roadmapType,
        ProjectStatus status,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getRoadmapType(),
                project.getStatus(),
                project.getCreatedAt()
        );
    }
}
