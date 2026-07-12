package com.example.seedbe.domain.prompt.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.prompt.component.PromptDiffCalculator;
import com.example.seedbe.domain.prompt.component.PromptVariableResolver;
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
    @Mock private PromptTemplate promptTemplate;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void createsProvidedPromptAndStartsStep() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubLockedStep(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.empty());
        when(promptTemplate.getActionPrompt()).thenReturn("주제: [topic]");
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

        ProjectStepPromptResponse response = service().createPrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("주제: 테스트 주제");
        assertThat(response.finalPrompt()).isEqualTo("주제: 테스트 주제");
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.IN_PROGRESS);
        verify(promptRepository).saveAndFlush(any(ProjectStepPrompt.class));
    }

    @Test
    void returnsExistingPromptWithoutDuplicateInsert() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = createPrompt(step, "provided");
        stubLockedStep(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(prompt));
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

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
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

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
        stubLockedStep(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(prompt));
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

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
        stubLockedStep(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().updatePrompt(
                userId, projectId, "constraint_analysis", "edited"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.GENERATED_PROMPT_NOT_FOUND);
    }

    private ProjectStepPromptService service() {
        return new ProjectStepPromptService(projectValidator, stepRepository, promptRepository,
                new PromptDiffCalculator(), new PromptVariableResolver());
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

    private void stubLockedStep(Project project, ProjectStep step) {
        stubContext(project);
        when(stepRepository.findByProjectAndRoadmapStepForUpdate(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

    private void stubStep(Project project, ProjectStep step) {
        stubContext(project);
        when(stepRepository.findByProjectAndRoadmapStepWithPromptTemplate(
                project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

}
