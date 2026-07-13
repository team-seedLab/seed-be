package com.example.seedbe.domain.selfcheck.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.selfcheck.dto.ProjectStepSelfCheckResponse;
import com.example.seedbe.domain.selfcheck.dto.SelfCheckItemRequest;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import com.example.seedbe.domain.selfcheck.enums.SelfCheckQuestionType;
import com.example.seedbe.domain.selfcheck.model.SelfCheckItem;
import com.example.seedbe.domain.selfcheck.repository.ProjectStepSelfCheckRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepSelfCheckService {
    private static final int MIN_ANSWER_CODE_POINT_COUNT = 10;

    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepResultRepository resultRepository;
    private final ProjectStepSelfCheckRepository selfCheckRepository;

    @Transactional
    public ProjectStepSelfCheckResponse saveSelfCheck(UUID userId, UUID projectId, String stepCodeStr,
                                                      List<SelfCheckItemRequest> requestedItems) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        ProjectStep step = stepRepository.findByProjectAndRoadmapStepForUpdate(context.project(), context.step())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        if (!promptRepository.existsByStep(step)) {
            throw new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND);
        }
        ProjectStepResult result = resultRepository.findByStep(step)
                .orElseThrow(() -> new BusinessException(ErrorType.GENERATED_RESULT_NOT_FOUND));
        if (result.getContentMarkdown() == null || result.getContentMarkdown().isBlank()) {
            throw new BusinessException(ErrorType.GENERATED_RESULT_NOT_FOUND);
        }

        List<SelfCheckItem> checkItems = validateAndNormalize(requestedItems);
        ProjectStepSelfCheck selfCheck = selfCheckRepository.findByStep(step)
                .map(existing -> {
                    existing.overwrite(checkItems);
                    return existing;
                })
                .orElseGet(() -> ProjectStepSelfCheck.builder()
                        .step(step)
                        .checkItems(checkItems)
                        .build());
        selfCheckRepository.saveAndFlush(selfCheck);
        step.complete();
        return ProjectStepSelfCheckResponse.of(step, selfCheck);
    }

    @Transactional(readOnly = true)
    public ProjectStepSelfCheckResponse getSelfCheck(UUID userId, UUID projectId, String stepCodeStr) {
        ValidatedContext context = validateAndGetContext(userId, projectId, stepCodeStr);
        ProjectStep step = stepRepository.findByProjectAndRoadmapStep(context.project(), context.step())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        ProjectStepSelfCheck selfCheck = selfCheckRepository.findByStep(step)
                .orElse(null);
        return selfCheck == null
                ? ProjectStepSelfCheckResponse.unanswered(step, getUnansweredItems())
                : ProjectStepSelfCheckResponse.of(step, selfCheck);
    }

    private List<SelfCheckItem> validateAndNormalize(List<SelfCheckItemRequest> requestedItems) {
        SelfCheckQuestionType[] questions = SelfCheckQuestionType.values();
        if (requestedItems == null || requestedItems.size() != questions.length) {
            throw new BusinessException(ErrorType.INVALID_SELF_CHECK_ITEMS);
        }

        Map<String, String> answersByKey = new HashMap<>();
        for (SelfCheckItemRequest item : requestedItems) {
            if (item == null || isBlank(item.key()) || isBlank(item.answer())
                    || answersByKey.putIfAbsent(item.key().strip(), item.answer()) != null) {
                throw new BusinessException(ErrorType.INVALID_SELF_CHECK_ITEMS);
            }
        }

        return Arrays.stream(questions)
                .map(question -> toSnapshot(question, answersByKey))
                .toList();
    }

    private List<SelfCheckItem> getUnansweredItems() {
        return Arrays.stream(SelfCheckQuestionType.values())
                .map(question -> new SelfCheckItem(question.getKey(), question.getQuestion(), null))
                .toList();
    }

    private SelfCheckItem toSnapshot(SelfCheckQuestionType question, Map<String, String> answersByKey) {
        String answer = answersByKey.get(question.getKey());
        if (answer == null) {
            throw new BusinessException(ErrorType.INVALID_SELF_CHECK_ITEMS);
        }
        long answerLength = answer.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
        if (answerLength < MIN_ANSWER_CODE_POINT_COUNT) {
            throw new BusinessException(ErrorType.SELF_CHECK_ANSWER_TOO_SHORT);
        }
        return new SelfCheckItem(question.getKey(), question.getQuestion(), answer);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
