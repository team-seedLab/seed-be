package com.example.seedbe.domain.project.dto;

import java.util.List;

public record ProjectDetailResponse(
        ProjectSummaryResponse summary,
        List<ProjectPromptStepResponse> stepResponses
) {
    public static ProjectDetailResponse of(ProjectSummaryResponse summary, List<ProjectPromptStepResponse> stepResponses) {
        return new ProjectDetailResponse(summary, stepResponses);
    }
}
