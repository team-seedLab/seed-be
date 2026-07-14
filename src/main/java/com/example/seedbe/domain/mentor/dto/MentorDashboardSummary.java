package com.example.seedbe.domain.mentor.dto;

public record MentorDashboardSummary(
        long totalStudentCount,
        long reviewingCount,
        long reviewedCount
) {
}
