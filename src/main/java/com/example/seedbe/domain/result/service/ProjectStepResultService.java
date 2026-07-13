package com.example.seedbe.domain.result.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.result.dto.ProjectStepResultResponse;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepResultService {
    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepResultRepository resultRepository;

    @Transactional
    public ProjectStepResultResponse saveResult(UUID userId, UUID projectId, String stepCodeStr,
                                                String contentMarkdown) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        ProjectStep step = stepRepository.findByProjectAndRoadmapStepForUpdate(
                        context.project(), context.step())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        if (!promptRepository.existsByStep(step)) {
            throw new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND);
        }

        ProjectStepResult result = resultRepository.findByStep(step)
                .map(existing -> {
                    existing.updateContent(contentMarkdown);
                    return existing;
                })
                .orElseGet(() -> ProjectStepResult.builder()
                        .step(step)
                        .contentMarkdown(contentMarkdown)
                        .build());
        resultRepository.saveAndFlush(result);
        return ProjectStepResultResponse.of(step, result);
    }

    @Transactional(readOnly = true)
    public ProjectStepResultResponse getResult(UUID userId, UUID projectId, String stepCodeStr) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        ProjectStep step = stepRepository.findByProjectAndRoadmapStep(context.project(), context.step())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        ProjectStepResult result = resultRepository.findByStep(step)
                .orElseThrow(() -> new BusinessException(ErrorType.GENERATED_RESULT_NOT_FOUND));
        return ProjectStepResultResponse.of(step, result);
    }

    private ValidatedContext validateAndGetContext(UUID userId, UUID projectId, String stepCodeStr) {
        RoadmapStep requestedStep = RoadmapStep.fromStepCode(stepCodeStr);
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        project.getRoadmapType().validateStep(requestedStep);
        return new ValidatedContext(project, requestedStep);
    }

    private record ValidatedContext(Project project, RoadmapStep step) {
    }
}
