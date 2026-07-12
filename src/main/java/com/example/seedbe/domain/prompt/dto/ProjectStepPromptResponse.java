package com.example.seedbe.domain.prompt.dto;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ProjectStepPromptResponse(
        UUID stepId,
        String stepCode,
        String stepName,
        String providedPromptSnapshot,
        String editedPrompt,
        String finalPrompt,
        String formatPrompt,
        int addedCount,
        int removedCount,
        Map<String, Object> diffJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectStepPromptResponse of(ProjectStep step, ProjectStepPrompt prompt) {
        String finalPrompt = prompt.getEditedPrompt() == null
                ? prompt.getProvidedPromptSnapshot()
                : prompt.getEditedPrompt();
        return new ProjectStepPromptResponse(step.getStepId(), step.getRoadmapStep().getStepCode(),
                step.getRoadmapStep().getDescription(), prompt.getProvidedPromptSnapshot(),
                prompt.getEditedPrompt(), finalPrompt, step.getPromptTemplate().getFormatPrompt(),
                prompt.getAddedCount(), prompt.getRemovedCount(), prompt.getDiffJson(),
                prompt.getCreatedAt(), prompt.getUpdatedAt());
    }
}
