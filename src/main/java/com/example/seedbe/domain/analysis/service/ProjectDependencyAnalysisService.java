package com.example.seedbe.domain.analysis.service;

import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.domain.aimentor.repository.ProjectStepAiMessageRepository;
import com.example.seedbe.domain.analysis.dto.ProjectDependencyAnalysisResponse;
import com.example.seedbe.domain.analysis.entity.ProjectDependencyAnalysis;
import com.example.seedbe.domain.analysis.repository.ProjectDependencyAnalysisRepository;
import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.seedbe.domain.aimentor.policy.AiMentorUsagePolicy.MAX_QUESTION_COUNT_PER_STEP;

@Service
@RequiredArgsConstructor
public class ProjectDependencyAnalysisService {
    private static final int EDIT_DEPTH_SATURATION_RATIO = 50;
    private static final float DEPENDENCY_USAGE_WEIGHT = 0.5f;
    private static final float DEPENDENCY_REASK_WEIGHT = 0.5f;
    private static final float INITIATIVE_EDIT_WEIGHT = 0.6f;
    private static final float INITIATIVE_LEARNING_QUESTION_WEIGHT = 0.4f;

    private final ProjectDependencyAnalysisRepository analysisRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepAiMessageRepository aiMessageRepository;
    private final ProjectValidator projectValidator;

    @Transactional(readOnly = true)
    public ProjectDependencyAnalysisResponse getAnalysis(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorType.PROJECT_NOT_COMPLETED);
        }
        return analysisRepository.findByProject(project)
                .map(ProjectDependencyAnalysisResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorType.DEPENDENCY_ANALYSIS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<ProjectDependencyAnalysisResponse> findByProject(Project project) {
        return analysisRepository.findByProject(project)
                .map(ProjectDependencyAnalysisResponse::from);
    }

    @Transactional
    public ProjectDependencyAnalysis analyzeAndSave(Project project, List<ProjectStep> steps) {
        List<ProjectStepPrompt> prompts = promptRepository.findByStepIn(steps);
        // Gemini 호출이 실패한 질문은 활용 지표에서 제외하고, 답변까지 저장된 turn만 집계한다.
        ProjectStepAiMessageRepository.AiUsage aiUsage =
                aiMessageRepository.aggregateCompletedUserMessages(
                        steps,
                        AiMessageSender.USER,
                        AiMessageSender.ASSISTANT,
                        AiMessageType.REASK_WITH_EDITED_PROMPT
                );

        AnalysisScores scores = calculateScores(steps.size(), prompts, aiUsage);
        ProjectDependencyAnalysis analysis = analysisRepository.findByProject(project)
                .map(existing -> {
                    existing.update(
                            scores.initiativeScore(),
                            scores.userEditRatio(),
                            scores.aiDependencyRatio()
                    );
                    return existing;
                })
                .orElseGet(() -> ProjectDependencyAnalysis.builder()
                        .project(project)
                        .initiativeScore(scores.initiativeScore())
                        .userEditRatio(scores.userEditRatio())
                        .aiDependencyRatio(scores.aiDependencyRatio())
                        .build());
        return analysisRepository.save(analysis);
    }

    private AnalysisScores calculateScores(
            int totalStepCount,
            List<ProjectStepPrompt> prompts,
            ProjectStepAiMessageRepository.AiUsage aiUsage
    ) {
        int userEditRatio = calculateUserEditRatio(prompts);
        long questionCount = aiUsage.getQuestionCount();
        int reaskRatio = percentage(aiUsage.getReaskCount(), questionCount);
        int aiDependencyRatio = calculateAiDependencyRatio(totalStepCount, questionCount, reaskRatio);
        int initiativeScore = calculateInitiativeScore(userEditRatio, questionCount, reaskRatio);

        return new AnalysisScores(initiativeScore, userEditRatio, aiDependencyRatio);
    }

    // 원본 전체 길이 중 사용자가 직접 추가하거나 삭제한 문자량의 비율이다.
    private int calculateUserEditRatio(List<ProjectStepPrompt> prompts) {
        long originalPromptLength = prompts.stream()
                .mapToLong(prompt -> codePointLength(prompt.getProvidedPromptSnapshot()))
                .sum();
        long addedCount = prompts.stream().mapToLong(ProjectStepPrompt::getAddedCount).sum();
        long removedCount = prompts.stream().mapToLong(ProjectStepPrompt::getRemovedCount).sum();
        return percentage(addedCount + removedCount, originalPromptLength);
    }

    // 질문을 많이 쓴 정도와 수정 프롬프트 재질문 비율을 절반씩 반영한다.
    private int calculateAiDependencyRatio(int totalStepCount, long questionCount, int reaskRatio) {
        int questionUsageRatio = percentage(questionCount,
                (long) totalStepCount * MAX_QUESTION_COUNT_PER_STEP);
        return weightedScore(
                questionUsageRatio, DEPENDENCY_USAGE_WEIGHT,
                reaskRatio, DEPENDENCY_REASK_WEIGHT
        );
    }

    // 수정 깊이를 중심으로 보고, AI를 썼다면 일반 학습 질문의 비율도 함께 반영한다.
    private int calculateInitiativeScore(int userEditRatio, long questionCount, int reaskRatio) {
        int editDepthScore = percentage(userEditRatio, EDIT_DEPTH_SATURATION_RATIO);
        return questionCount == 0
                ? editDepthScore
                : weightedScore(
                        editDepthScore, INITIATIVE_EDIT_WEIGHT,
                        100 - reaskRatio, INITIATIVE_LEARNING_QUESTION_WEIGHT
                );
    }

    private int percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(numerator * 100.0f / denominator));
    }

    private int codePointLength(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    private int weightedScore(int firstScore, float firstWeight, int secondScore, float secondWeight) {
        return Math.round(firstScore * firstWeight + secondScore * secondWeight);
    }

    private record AnalysisScores(
            int initiativeScore,
            int userEditRatio,
            int aiDependencyRatio
    ) {
    }
}
