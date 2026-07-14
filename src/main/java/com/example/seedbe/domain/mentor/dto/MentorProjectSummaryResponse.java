package com.example.seedbe.domain.mentor.dto;

import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MentorProjectSummaryResponse(
        UUID projectId,
        String title,
        RoadmapType roadmapType,
        ProjectStatus projectStatus,
        RoadmapStep currentRoadmapStep,
        Integer currentStepOrder,
        int totalStepCount,
        int completedStepCount,
        int progressPercent,
        ProjectReviewStatus reviewStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
