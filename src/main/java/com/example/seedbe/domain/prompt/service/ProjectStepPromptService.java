package com.example.seedbe.domain.prompt.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.component.PromptDiffCalculator;
import com.example.seedbe.domain.prompt.dto.ProjectStepPromptResponse;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepPromptService {
    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final PromptDiffCalculator promptDiffCalculator;

    @Transactional
    public ProjectStepPromptResponse createPrompt(UUID userId, UUID projectId, String stepCodeStr) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        Project project = context.project();
        ProjectStep step = getStepForUpdate(context);

        var existingPrompt = promptRepository.findByStep(step);
        if (existingPrompt.isPresent()) {
            step.start();
            return ProjectStepPromptResponse.of(step, existingPrompt.get());
        }

        String providedPrompt = replaceVariables(
                step.getPromptTemplate().getActionPrompt(), project.getInitialContext());
        ProjectStepPrompt prompt = ProjectStepPrompt.builder()
                .step(step)
                .providedPromptSnapshot(providedPrompt)
                .build();

        promptRepository.saveAndFlush(prompt);
        step.start();
        return ProjectStepPromptResponse.of(step, prompt);
    }

    @Transactional(readOnly = true)
    public ProjectStepPromptResponse getPrompt(UUID userId, UUID projectId, String stepCodeStr) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        ProjectStep step = stepRepository.findByProjectAndRoadmapStep(context.project(), context.step())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        ProjectStepPrompt prompt = promptRepository.findByStep(step)
                .orElseThrow(() -> new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND));
        return ProjectStepPromptResponse.of(step, prompt);
    }

    @Transactional
    public ProjectStepPromptResponse updatePrompt(UUID userId, UUID projectId, String stepCodeStr,
                                                  String editedPrompt) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        ProjectStep step = getStepForUpdate(context);
        ProjectStepPrompt prompt = promptRepository.findByStep(step)
                .orElseThrow(() -> new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND));
        PromptDiffCalculator.PromptDiff diff = promptDiffCalculator.calculate(
                prompt.getProvidedPromptSnapshot(), editedPrompt);
        prompt.updateEditedPrompt(editedPrompt, diff.addedCount(), diff.removedCount(), diff.diffJson());
        return ProjectStepPromptResponse.of(step, prompt);
    }

    private ProjectStep getStepForUpdate(ValidatedContext context) {
        return stepRepository.findByProjectAndRoadmapStepForUpdate(context.project(), context.step())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
    }

    private String replaceVariables(String promptTemplate, Map<String, Object> initialContext) {
        String result = promptTemplate;
        if (initialContext == null || initialContext.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Object> entry : initialContext.entrySet()) {
            result = result.replace("[" + entry.getKey() + "]", String.valueOf(entry.getValue()));
        }
        return result;
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
