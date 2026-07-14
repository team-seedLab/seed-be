package com.example.seedbe.domain.mentor.dto;

import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.selfcheck.model.SelfCheckItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MentorProjectStepDetailResponse(
        UUID stepId,
        String stepCode,
        String stepName,
        Integer stepOrder,
        ProjectStepStatus status,
        LocalDateTime completedAt,
        PromptDetail prompt,
        ResultDetail result,
        SelfCheckDetail selfCheck
) {
    public record PromptDetail(
            String providedPromptSnapshot,
            String editedPrompt,
            Integer addedCount,
            Integer removedCount,
            Map<String, Object> diffJson
    ) {
    }

    public record ResultDetail(
            String contentMarkdown
    ) {
    }

    public record SelfCheckDetail(
            List<SelfCheckItem> checkItems,
            LocalDateTime submittedAt
    ) {
    }
}
