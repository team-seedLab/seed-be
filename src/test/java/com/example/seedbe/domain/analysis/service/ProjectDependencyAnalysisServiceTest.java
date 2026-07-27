package com.example.seedbe.domain.analysis.service;

import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.domain.aimentor.repository.ProjectStepAiMessageRepository;
import com.example.seedbe.domain.analysis.entity.ProjectDependencyAnalysis;
import com.example.seedbe.domain.analysis.repository.ProjectDependencyAnalysisRepository;
import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDependencyAnalysisServiceTest {

    @Mock private ProjectDependencyAnalysisRepository analysisRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepAiMessageRepository aiMessageRepository;
    @Mock private ProjectValidator projectValidator;

    @Test
    void returnsAnalysisForOwnedCompletedProject() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = project(ProjectStatus.COMPLETED);
        ProjectDependencyAnalysis analysis = ProjectDependencyAnalysis.builder()
                .project(project)
                .initiativeScore(70)
                .userEditRatio(40)
                .aiDependencyRatio(30)
                .build();
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(analysisRepository.findByProject(project)).thenReturn(Optional.of(analysis));

        var response = service().getAnalysis(userId, projectId);

        assertThat(response.initiativeScore()).isEqualTo(70);
        assertThat(response.userEditRatio()).isEqualTo(40);
        assertThat(response.aiDependencyRatio()).isEqualTo(30);
    }

    @Test
    void rejectsAnalysisForInProgressProject() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = project(ProjectStatus.IN_PROGRESS);
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);

        assertThatThrownBy(() -> service().getAnalysis(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.PROJECT_NOT_COMPLETED);
        verify(analysisRepository, never()).findByProject(any());
    }

    @Test
    void rejectsMissingAnalysisForCompletedProject() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = project(ProjectStatus.COMPLETED);
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(analysisRepository.findByProject(project)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getAnalysis(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.DEPENDENCY_ANALYSIS_NOT_FOUND);
    }

    @Test
    void calculatesAndSavesProjectUsageMetrics() {
        Project project = project();
        ProjectStep first = step(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        ProjectStep second = step(project, RoadmapStep.ARGUMENT_STRUCTURING, 2);
        List<ProjectStep> steps = List.of(first, second);
        ProjectStepPrompt editedPrompt = ProjectStepPrompt.builder()
                .step(first)
                .providedPromptSnapshot("a".repeat(100))
                .editedPrompt("수정 프롬프트")
                .addedCount(30)
                .removedCount(10)
                .build();
        ProjectStepPrompt originalPrompt = ProjectStepPrompt.builder()
                .step(second)
                .providedPromptSnapshot("b".repeat(100))
                .build();
        ProjectStepAiMessageRepository.AiUsage aiUsage = usage(5, 1);

        when(promptRepository.findByStepIn(steps)).thenReturn(List.of(editedPrompt, originalPrompt));
        when(aiMessageRepository.aggregateCompletedUserMessages(
                steps, AiMessageSender.USER, AiMessageSender.ASSISTANT,
                AiMessageType.REASK_WITH_EDITED_PROMPT))
                .thenReturn(aiUsage);
        when(analysisRepository.findByProject(project)).thenReturn(Optional.empty());
        when(analysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDependencyAnalysis analysis = service().analyzeAndSave(project, steps);

        assertThat(analysis.getUserEditRatio()).isEqualTo(20);
        assertThat(analysis.getAiDependencyRatio()).isEqualTo(80);
        assertThat(analysis.getInitiativeScore()).isEqualTo(56);
    }

    @Test
    void usesAiMentorDependencyWhenPromptIsFullyEdited() {
        Project project = project();
        ProjectStep step = step(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        List<ProjectStep> steps = List.of(step);
        ProjectStepPrompt fullyEditedPrompt = ProjectStepPrompt.builder()
                .step(step)
                .providedPromptSnapshot("a".repeat(100))
                .editedPrompt("직접 다시 작성한 프롬프트")
                .addedCount(100)
                .build();
        ProjectStepAiMessageRepository.AiUsage aiUsage = usage(10, 0);
        when(promptRepository.findByStepIn(steps)).thenReturn(List.of(fullyEditedPrompt));
        when(aiMessageRepository.aggregateCompletedUserMessages(
                steps, AiMessageSender.USER, AiMessageSender.ASSISTANT,
                AiMessageType.REASK_WITH_EDITED_PROMPT)).thenReturn(aiUsage);
        when(analysisRepository.findByProject(project)).thenReturn(Optional.empty());
        when(analysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDependencyAnalysis analysis = service().analyzeAndSave(project, steps);

        assertThat(analysis.getUserEditRatio()).isEqualTo(100);
        assertThat(analysis.getAiDependencyRatio()).isEqualTo(50);
    }

    @Test
    void setsDependencyToZeroWhenPromptIsFullyEditedWithoutAiUsage() {
        Project project = project();
        ProjectStep step = step(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        List<ProjectStep> steps = List.of(step);
        ProjectStepPrompt fullyEditedPrompt = ProjectStepPrompt.builder()
                .step(step)
                .providedPromptSnapshot("a".repeat(100))
                .editedPrompt("직접 다시 작성한 프롬프트")
                .addedCount(100)
                .build();
        ProjectStepAiMessageRepository.AiUsage noUsage = usage(0, 0);
        when(promptRepository.findByStepIn(steps)).thenReturn(List.of(fullyEditedPrompt));
        when(aiMessageRepository.aggregateCompletedUserMessages(
                steps, AiMessageSender.USER, AiMessageSender.ASSISTANT,
                AiMessageType.REASK_WITH_EDITED_PROMPT)).thenReturn(noUsage);
        when(analysisRepository.findByProject(project)).thenReturn(Optional.empty());
        when(analysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDependencyAnalysis analysis = service().analyzeAndSave(project, steps);

        assertThat(analysis.getUserEditRatio()).isEqualTo(100);
        assertThat(analysis.getAiDependencyRatio()).isZero();
    }

    @Test
    void updatesExistingAnalysisInsteadOfCreatingDuplicate() {
        Project project = project();
        ProjectStep step = step(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        List<ProjectStep> steps = List.of(step);
        ProjectDependencyAnalysis existing = ProjectDependencyAnalysis.builder()
                .project(project)
                .initiativeScore(1)
                .userEditRatio(1)
                .aiDependencyRatio(1)
                .build();
        ProjectStepAiMessageRepository.AiUsage noUsage = usage(0, 0);
        when(promptRepository.findByStepIn(steps)).thenReturn(List.of());
        when(aiMessageRepository.aggregateCompletedUserMessages(
                steps, AiMessageSender.USER, AiMessageSender.ASSISTANT,
                AiMessageType.REASK_WITH_EDITED_PROMPT)).thenReturn(noUsage);
        when(analysisRepository.findByProject(project)).thenReturn(Optional.of(existing));
        when(analysisRepository.save(existing)).thenReturn(existing);

        ProjectDependencyAnalysis saved = service().analyzeAndSave(project, steps);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getInitiativeScore()).isZero();
        assertThat(saved.getUserEditRatio()).isZero();
        assertThat(saved.getAiDependencyRatio()).isEqualTo(100);
        verify(analysisRepository).save(existing);
    }

    private ProjectDependencyAnalysisService service() {
        return new ProjectDependencyAnalysisService(
                analysisRepository, promptRepository, aiMessageRepository, projectValidator);
    }

    private Project project() {
        return project(null);
    }

    private Project project(ProjectStatus status) {
        Project project = Project.builder()
                .title("분석 프로젝트")
                .roadmapType(RoadmapType.REPORT)
                .status(status)
                .build();
        ReflectionTestUtils.setField(project, "projectId", UUID.randomUUID());
        return project;
    }

    private ProjectStep step(Project project, RoadmapStep roadmapStep, int order) {
        ProjectStep step = ProjectStep.builder()
                .project(project)
                .promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(roadmapStep)
                .stepOrder(order)
                .build();
        ReflectionTestUtils.setField(step, "stepId", UUID.randomUUID());
        return step;
    }

    private ProjectStepAiMessageRepository.AiUsage usage(long questions, long reasks) {
        ProjectStepAiMessageRepository.AiUsage usage = mock(ProjectStepAiMessageRepository.AiUsage.class);
        when(usage.getQuestionCount()).thenReturn(questions);
        when(usage.getReaskCount()).thenReturn(reasks);
        return usage;
    }
}
