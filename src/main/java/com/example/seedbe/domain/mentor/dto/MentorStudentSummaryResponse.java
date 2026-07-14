package com.example.seedbe.domain.mentor.dto;

import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MentorStudentSummaryResponse(
        UUID studentId,
        String nickname,
        String email,
        String profileUrl,
        int totalProjectCount,
        int inProgressProjectCount,
        int completedProjectCount,
        ProjectReviewStatus reviewStatus,
        LocalDateTime lastProjectUpdatedAt
) {
}
