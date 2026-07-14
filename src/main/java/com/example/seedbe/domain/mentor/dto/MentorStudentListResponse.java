package com.example.seedbe.domain.mentor.dto;

import java.util.List;

public record MentorStudentListResponse(
        MentorDashboardSummary summary,
        List<MentorStudentSummaryResponse> students
) {
}
