package com.example.seedbe.domain.prompt.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.prompt.component.PromptDiffCalculator;
import com.example.seedbe.domain.prompt.dto.ProjectStepPromptResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.selfcheck.repository.ProjectStepSelfCheckRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStepPromptServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepSelfCheckRepository selfCheckRepository;
    @Mock private PromptTemplate promptTemplate;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void createsProvidedPromptAndStartsStep() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubStepWithPromptTemplateForUpdate(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.empty());
        when(promptTemplate.getActionPrompt()).thenReturn("주제: [topic]");

        ProjectStepPromptResponse response = service().createPrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("주제: [topic]");
        assertThat(response.finalPrompt()).isEqualTo("주제: [topic]");
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.IN_PROGRESS);
        verify(promptRepository).saveAndFlush(any(ProjectStepPrompt.class));
    }

    @Test
    void storesActionPromptWithoutEmbeddingProjectContext() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubStepWithPromptTemplateForUpdate(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.empty());
        when(promptTemplate.getActionPrompt()).thenReturn("핵심 개념: [핵심 개념]");

        ProjectStepPromptResponse response = service().createPrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("핵심 개념: [핵심 개념]");
        assertThat(response.providedPromptSnapshot()).doesNotContain("테스트 주제");
    }

    @Test
    void returnsExistingPromptWithoutDuplicateInsert() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = createPrompt(step, "provided");
        stubStepWithPromptTemplateForUpdate(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(prompt));

        ProjectStepPromptResponse response = service().createPrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("provided");
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.IN_PROGRESS);
        verify(promptRepository, never()).saveAndFlush(any());
    }

    @Test
    void getsPromptDirectlyFromPromptRepository() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = createPrompt(step, "provided");
        stubStep(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(prompt));

        ProjectStepPromptResponse response = service().getPrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("provided");
        assertThat(response.editedPrompt()).isNull();
    }

    @Test
    void updatesEditedPromptAndDiffAgainstOriginalSnapshot() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = createPrompt(step, "hello world");
        stubStepForUpdate(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(prompt));

        ProjectStepPromptResponse response = service().updatePrompt(
                userId, projectId, "constraint_analysis", "hello brave world");

        assertThat(response.editedPrompt()).isEqualTo("hello brave world");
        assertThat(response.finalPrompt()).isEqualTo("hello brave world");
        assertThat(response.addedCount()).isEqualTo(6);
        assertThat(response.removedCount()).isZero();
        assertThat(response.diffJson()).containsEntry("version", "PREFIX_SUFFIX_V1");
    }

    @Test
    void rejectsUpdateBeforePromptCreation() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubStepForUpdate(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().updatePrompt(
                userId, projectId, "constraint_analysis", "edited"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.GENERATED_PROMPT_NOT_FOUND);
    }

    @Test
    void rejectsStartingNextStepBeforePreviousStepCompletion() {
        Project project = createProject();
        ProjectStep previousStep = createStep(project);
        ProjectStep nextStep = ProjectStep.builder().project(project).promptTemplate(promptTemplate)
                .roadmapStep(RoadmapStep.ARGUMENT_STRUCTURING).stepOrder(2).build();
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepWithPromptTemplateForUpdate(
                project, RoadmapStep.ARGUMENT_STRUCTURING)).thenReturn(Optional.of(nextStep));
        when(stepRepository.findByProjectAndStepOrder(project, 1)).thenReturn(Optional.of(previousStep));

        assertThatThrownBy(() -> service().createPrompt(userId, projectId, "argument_structuring"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.PREVIOUS_STEP_NOT_COMPLETED);
        verify(promptRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStartingNextStepWhenPreviousSelfCheckIsMissing() {
        Project project = createProject();
        ProjectStep previousStep = createStep(project);
        previousStep.complete();
        ProjectStep nextStep = ProjectStep.builder().project(project).promptTemplate(promptTemplate)
                .roadmapStep(RoadmapStep.ARGUMENT_STRUCTURING).stepOrder(2).build();
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepWithPromptTemplateForUpdate(
                project, RoadmapStep.ARGUMENT_STRUCTURING)).thenReturn(Optional.of(nextStep));
        when(stepRepository.findByProjectAndStepOrder(project, 1)).thenReturn(Optional.of(previousStep));
        when(selfCheckRepository.existsByStep(previousStep)).thenReturn(false);

        assertThatThrownBy(() -> service().createPrompt(userId, projectId, "argument_structuring"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.PREVIOUS_STEP_NOT_COMPLETED);
    }

    @Test
    void returnsLegacyExistingPromptWithoutPreviousSelfCheck() {
        Project project = createProject();
        ProjectStep nextStep = ProjectStep.builder().project(project).promptTemplate(promptTemplate)
                .roadmapStep(RoadmapStep.ARGUMENT_STRUCTURING).stepOrder(2).build();
        ProjectStepPrompt existingPrompt = createPrompt(nextStep, "legacy provided");
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepWithPromptTemplateForUpdate(
                project, RoadmapStep.ARGUMENT_STRUCTURING)).thenReturn(Optional.of(nextStep));
        when(promptRepository.findByStep(nextStep)).thenReturn(Optional.of(existingPrompt));

        ProjectStepPromptResponse response = service().createPrompt(
                userId, projectId, "argument_structuring");

        assertThat(response.providedPromptSnapshot()).isEqualTo("legacy provided");
        assertThat(nextStep.getStatus()).isEqualTo(ProjectStepStatus.IN_PROGRESS);
        verify(stepRepository, never()).findByProjectAndStepOrder(any(), any());
        verify(selfCheckRepository, never()).existsByStep(any());
    }

    private ProjectStepPromptService service() {
        return new ProjectStepPromptService(projectValidator, stepRepository, promptRepository,
                selfCheckRepository, new PromptDiffCalculator());
    }

    private Project createProject() {
        return Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of("topic", "테스트 주제")).build();
    }

    private ProjectStep createStep(Project project) {
        return ProjectStep.builder().project(project).promptTemplate(promptTemplate)
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
    }

    private ProjectStepPrompt createPrompt(ProjectStep step, String provided) {
        return ProjectStepPrompt.builder().step(step).providedPromptSnapshot(provided).build();
    }

    private void stubContext(Project project) {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
    }

    private void stubStepWithPromptTemplateForUpdate(Project project, ProjectStep step) {
        stubContext(project);
        when(stepRepository.findByProjectAndRoadmapStepWithPromptTemplateForUpdate(
                project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

    private void stubStepForUpdate(Project project, ProjectStep step) {
        stubContext(project);
        when(stepRepository.findByProjectAndRoadmapStepForUpdate(
                project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

    private void stubStep(Project project, ProjectStep step) {
        stubContext(project);
        when(stepRepository.findByProjectAndRoadmapStep(
                project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

}
