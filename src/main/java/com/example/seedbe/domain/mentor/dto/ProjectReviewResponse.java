package com.example.seedbe.domain.mentor.dto;

import com.example.seedbe.domain.mentor.entity.ProjectReview;
import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectReviewResponse(
        UUID projectReviewId,
        UUID projectId,
        ProjectReviewStatus status,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectReviewResponse from(ProjectReview review) {
        return new ProjectReviewResponse(
                review.getProjectReviewId(),
                review.getProject().getProjectId(),
                review.getStatus(),
                review.getReviewedAt(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
