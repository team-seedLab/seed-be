package com.example.seedbe.domain.selfcheck.dto;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import com.example.seedbe.domain.selfcheck.model.SelfCheckItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectStepSelfCheckResponse(
        UUID selfCheckId,
        UUID stepId,
        String stepCode,
        String stepName,
        List<SelfCheckItem> checkItems,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectStepSelfCheckResponse of(ProjectStep step, ProjectStepSelfCheck selfCheck) {
        return new ProjectStepSelfCheckResponse(
                selfCheck.getSelfCheckId(),
                step.getStepId(),
                step.getRoadmapStep().getStepCode(),
                step.getRoadmapStep().getDescription(),
                selfCheck.getCheckItems(),
                selfCheck.getSubmittedAt(),
                selfCheck.getCreatedAt(),
                selfCheck.getUpdatedAt()
        );
    }

    public static ProjectStepSelfCheckResponse unanswered(ProjectStep step, List<SelfCheckItem> checkItems) {
        return new ProjectStepSelfCheckResponse(
                null,
                step.getStepId(),
                step.getRoadmapStep().getStepCode(),
                step.getRoadmapStep().getDescription(),
                checkItems,
                null,
                null,
                null
        );
    }
}
