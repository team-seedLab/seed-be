package com.example.seedbe.domain.result.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.result.dto.ProjectStepResultResponse;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
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
class ProjectStepResultServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepResultRepository resultRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void createsResultWithoutCompletingStep() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        step.start();
        stubLockedStep(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.empty());
        when(resultRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectStepResultResponse response = service().saveResult(
                userId, projectId, "constraint_analysis", "# 결과물");

        assertThat(response.contentMarkdown()).isEqualTo("# 결과물");
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.IN_PROGRESS);
        verify(resultRepository).saveAndFlush(any(ProjectStepResult.class));
    }

    @Test
    void overwritesExistingResult() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepResult result = ProjectStepResult.builder().step(step).contentMarkdown("old").build();
        stubLockedStep(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.of(result));
        when(resultRepository.saveAndFlush(result)).thenReturn(result);

        ProjectStepResultResponse response = service().saveResult(
                userId, projectId, "constraint_analysis", "new");

        assertThat(response.contentMarkdown()).isEqualTo("new");
        assertThat(result.getContentMarkdown()).isEqualTo("new");
    }

    @Test
    void getsSavedResult() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepResult result = ProjectStepResult.builder().step(step).contentMarkdown("saved").build();
        stubStep(project, step);
        when(resultRepository.findByStep(step)).thenReturn(Optional.of(result));

        assertThat(service().getResult(userId, projectId, "constraint_analysis").contentMarkdown())
                .isEqualTo("saved");
    }

    @Test
    void rejectsSaveBeforePromptCreation() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubLockedStep(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(false);

        assertThatThrownBy(() -> service().saveResult(
                userId, projectId, "constraint_analysis", "result"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.GENERATED_PROMPT_NOT_FOUND);
        verify(resultRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAccessToAnotherUsersProject() {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId))
                .thenThrow(new BusinessException(ErrorType.FORBIDDEN_ACCESS));

        assertThatThrownBy(() -> service().getResult(
                userId, projectId, "constraint_analysis"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.FORBIDDEN_ACCESS);
        verify(stepRepository, never()).findByProjectAndRoadmapStep(any(), any());
    }

    @Test
    void rejectsGetWhenResultDoesNotExist() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubStep(project, step);
        when(resultRepository.findByStep(step)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getResult(
                userId, projectId, "constraint_analysis"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.GENERATED_RESULT_NOT_FOUND);
    }

    private ProjectStepResultService service() {
        return new ProjectStepResultService(projectValidator, stepRepository, promptRepository, resultRepository);
    }

    private Project createProject() {
        return Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
    }

    private ProjectStep createStep(Project project) {
        return ProjectStep.builder().project(project).promptTemplate(org.mockito.Mockito.mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
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
        when(stepRepository.findByProjectAndRoadmapStep(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }
}
