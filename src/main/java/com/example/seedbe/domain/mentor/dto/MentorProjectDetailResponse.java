package com.example.seedbe.domain.mentor.dto;

import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MentorProjectDetailResponse(
        UUID projectId,
        UUID studentId,
        String studentNickname,
        String title,
        RoadmapType roadmapType,
        ProjectStatus projectStatus,
        String desiredOutcome,
        String keyFocus,
        String requiredElements,
        ProjectReviewStatus reviewStatus,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        List<MentorProjectStepDetailResponse> steps
) {
}
