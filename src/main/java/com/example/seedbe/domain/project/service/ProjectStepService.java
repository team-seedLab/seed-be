package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.entity.ProjectStepPrompt;
import com.example.seedbe.domain.project.entity.ProjectStepResult;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.project.repository.ProjectStepResultRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepService {
    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepResultRepository resultRepository;

    public ProjectPromptStepResponse createAndSavePrompt(UUID userId, UUID projectId, String stepCodeStr) {
        // 유효성 검사
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        Project project = context.project();
        RoadmapStep requestedStep = context.step();

        ProjectStep step = stepRepository.findByProjectAndRoadmapStepWithDetails(project, requestedStep)
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));

        if (step.getPrompt() != null) {
            return ProjectPromptStepResponse.from(step);
        }

        // 존재하지 않으면 새로 생성후 DTO로 반환
        String finalActionPrompt = replaceVariables(step.getPromptTemplate().getActionPrompt(), project.getInitialContext());

        ProjectStepPrompt prompt = ProjectStepPrompt.builder()
                .step(step)
                .providedPromptSnapshot(finalActionPrompt)
                .build();

        try {
            promptRepository.saveAndFlush(prompt);
        } catch (DataIntegrityViolationException e) {
            ProjectStep concurrentStep = stepRepository.findByProjectAndRoadmapStepWithDetails(project, requestedStep)
                    .orElseThrow(() -> e);
            return ProjectPromptStepResponse.from(concurrentStep);
        }

        step.start(prompt);
        stepRepository.save(step);
        return ProjectPromptStepResponse.from(step);
    }

    @Transactional
    public void saveStepResult(UUID userId, UUID projectId, String stepCodeStr, String resultText) {
        // 유효성 검사
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        Project project = context.project();
        RoadmapStep requestedStep = context.step();

        ProjectStep step = stepRepository.findByProjectAndRoadmapStepWithDetails(project, requestedStep)
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));

        if (step.getPrompt() == null || step.getPrompt().getProvidedPromptSnapshot().isBlank()) {
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


    // JSON(Map) 변수 치환기
    private String replaceVariables(String promptTemplate, Map<String, Object> initialContextMap) {
        String result = promptTemplate;

        // Map의 Key-Value를 돌면서 템플릿의 [key]를 value로 즉시 치환
        for (Map.Entry<String, Object> entry : initialContextMap.entrySet()) {
            String targetKey = "[" + entry.getKey() + "]";
            String replaceValue = String.valueOf(entry.getValue());

            result = result.replace(targetKey, replaceValue);
        }

        return result;
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
