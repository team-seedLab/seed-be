package com.example.seedbe.domain.mentor.dto;

import java.util.List;
import java.util.UUID;

public record MentorStudentProjectListResponse(
        UUID studentId,
        String nickname,
        String email,
        String profileUrl,
        List<MentorProjectSummaryResponse> projects
) {
}
