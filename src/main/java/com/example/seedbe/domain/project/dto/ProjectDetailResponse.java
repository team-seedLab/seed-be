package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ProjectDetailResponse(
        UUID projectId,
        String title,
        RoadmapType roadmapType,
        ProjectStatus status,
        Map<String, Object> initialContext, // 단건 조회이므로 상세한 jsonb 데이터를 모두 내려줌
        LocalDateTime createdAt
) {
    public static ProjectDetailResponse from(Project project) {
        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getRoadmapType(),
                project.getStatus(),
                project.getInitialContext(),
                project.getCreatedAt()
        );
    }
}
