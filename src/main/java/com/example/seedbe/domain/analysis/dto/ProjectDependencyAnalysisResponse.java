package com.example.seedbe.domain.analysis.dto;

import com.example.seedbe.domain.analysis.entity.ProjectDependencyAnalysis;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectDependencyAnalysisResponse(
        UUID dependencyAnalysisId,
        int initiativeScore,
        int userEditRatio,
        int aiDependencyRatio,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectDependencyAnalysisResponse from(ProjectDependencyAnalysis analysis) {
        return new ProjectDependencyAnalysisResponse(
                analysis.getDependencyAnalysisId(),
                analysis.getInitiativeScore(),
                analysis.getUserEditRatio(),
                analysis.getAiDependencyRatio(),
                analysis.getCreatedAt(),
                analysis.getUpdatedAt()
        );
    }
}
