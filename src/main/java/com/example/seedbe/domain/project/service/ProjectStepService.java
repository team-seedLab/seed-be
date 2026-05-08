package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStepLog;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectStepLogRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.PromptTemplateRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepService {
    private final ProjectValidator projectValidator;
    private final PromptTemplateRepository templateRepository;
    private final ProjectStepLogRepository stepLogRepository;

    public ProjectPromptStepResponse createAndSavePrompt(UUID userId, UUID projectId, String stepCodeStr) {
        // 유효성 검사
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        Project project = context.project();
        RoadmapStep requestedStep = context.step();

        Optional<ProjectStepLog> existingLog = stepLogRepository.findByProjectAndRoadmapStepWithPromptTemplate(
                project, requestedStep);

        // 존재하면 그대로 DTO로 반환
        if (existingLog.isPresent()) {
            return ProjectPromptStepResponse.from(existingLog.get());
        }

        PromptTemplate template = templateRepository.findByRoadmapTypeAndRoadmapStepAndIsActiveTrue(
                        project.getRoadmapType(), requestedStep)
                .orElseThrow(() -> new BusinessException(ErrorType.PROMPT_TEMPLATE_NOT_FOUND));

        // 존재하지 않으면 새로 생성후 DTO로 반환
        String finalActionPrompt = replaceVariables(template.getActionPrompt(), project.getInitialContext());

        ProjectStepLog newLog = ProjectStepLog.builder()
                .project(project)
                .promptTemplate(template)
                .roadmapStep(requestedStep)
                .providedPromptSnapshot(finalActionPrompt)
                .build();

        try {
            stepLogRepository.saveAndFlush(newLog);
        } catch (DataIntegrityViolationException e) {
            ProjectStepLog concurrentLog = stepLogRepository.findByProjectAndRoadmapStepWithPromptTemplate(
                            project, requestedStep)
                    .orElseThrow(() -> e);
            return ProjectPromptStepResponse.from(concurrentLog);
        }

        return ProjectPromptStepResponse.from(newLog);
    }

    @Transactional
    public void saveStepResult(UUID userId, UUID projectId, String stepCodeStr, String resultText) {
        // 유효성 검사
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        Project project = context.project();
        RoadmapStep requestedStep = context.step();

        ProjectStepLog stepLog = stepLogRepository.findByProjectAndRoadmapStep(project, requestedStep)
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));

        if (stepLog.getProvidedPromptSnapshot().isBlank()) {
            throw new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND);
        }

        stepLog.updateSubmittedResult(resultText);
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
