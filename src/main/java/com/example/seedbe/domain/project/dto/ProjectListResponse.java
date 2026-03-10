package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.Project;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectListResponse(
        UUID projectId,
        String title,
        String roadmapType,
        String status,
        LocalDateTime createdAt
) {
    public static ProjectListResponse from(Project project) {
        return new ProjectListResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getRoadmapType().name(),
                project.getStatus().name(),
                project.getCreatedAt()
        );
    }
}
