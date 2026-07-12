package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.entity.ProjectStepResult;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.project.repository.ProjectStepResultRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepService {
    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepResultRepository resultRepository;
    @Transactional
    public void saveStepResult(UUID userId, UUID projectId, String stepCodeStr, String resultText) {
        // 유효성 검사
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        Project project = context.project();
        RoadmapStep requestedStep = context.step();

        ProjectStep step = stepRepository.findByProjectAndRoadmapStepWithDetails(project, requestedStep)
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));

        if (promptRepository.findByStep(step).isEmpty()) {
            throw new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND);
        }

        ProjectStepResult result = step.getResult();
        if (result == null) {
            result = ProjectStepResult.builder()
                    .step(step)
                    .contentMarkdown(resultText)
                    .build();
            resultRepository.save(result);
        } else {
            result.updateContent(resultText);
        }
        step.complete(result);
    }
    // 공통 검증 헬퍼 메서드
    private record ValidatedContext(Project project, RoadmapStep step) {}

    private ValidatedContext validateAndGetContext(UUID userId, UUID projectId, String stepCodeStr) {
        RoadmapStep requestedStep = RoadmapStep.fromStepCode(stepCodeStr);

        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        project.getRoadmapType().validateStep(requestedStep);

        return new ValidatedContext(project, requestedStep);
    }
}
