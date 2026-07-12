package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.entity.ProjectStepPrompt;
import com.example.seedbe.domain.project.entity.ProjectStepResult;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.project.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStepServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepResultRepository resultRepository;
    @Mock private PromptTemplate promptTemplate;

    @Test
    @DisplayName("이미 발급된 단계는 prompt를 다시 저장하지 않고 기존 응답을 반환한다.")
    void createAndSavePromptReturnsExistingPrompt() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = ProjectStepPrompt.builder()
                .step(step).providedPromptSnapshot("prompt").build();
        step.start(prompt);
        ProjectStepResult result = ProjectStepResult.builder().step(step).contentMarkdown("result").build();
        step.complete(result);
        stubStepLookup(project, step);
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

        ProjectPromptStepResponse response = service().createAndSavePrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("prompt");
        assertThat(response.formatPrompt()).isEqualTo("format");
        assertThat(response.userSubmittedResult()).isEqualTo("result");
        verify(promptRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("단계 시작 시 제공 프롬프트를 최초 저장한다.")
    void createAndSavePromptSavesPrompt() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubStepLookup(project, step);
        when(promptTemplate.getActionPrompt()).thenReturn("주제: [topic]");
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

        ProjectPromptStepResponse response = service().createAndSavePrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("주제: 테스트 주제");
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.IN_PROGRESS);
        verify(promptRepository).saveAndFlush(any(ProjectStepPrompt.class));
        verify(stepRepository).save(step);
    }

    @Test
    @DisplayName("동시 prompt 생성 unique 충돌 시 이미 생성된 prompt를 재조회한다.")
    void createAndSavePromptReturnsConcurrentPrompt() {
        Project project = createProject();
        ProjectStep initialStep = createStep(project);
        ProjectStep concurrentStep = createStep(project);
        concurrentStep.start(ProjectStepPrompt.builder().step(concurrentStep)
                .providedPromptSnapshot("concurrent prompt").build());
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepWithDetails(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(initialStep), Optional.of(concurrentStep));
        when(promptTemplate.getActionPrompt()).thenReturn("prompt");
        when(promptTemplate.getFormatPrompt()).thenReturn("format");
        when(promptRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ProjectPromptStepResponse response = service().createAndSavePrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("concurrent prompt");
        verify(stepRepository, never()).save(any());
    }

    @Test
    @DisplayName("결과물을 최초 저장한다.")
    void saveStepResultCreatesResult() {
        Project project = createProject();
        ProjectStep step = startedStep(project);
        stubStepLookup(project, step);

        service().saveStepResult(userId, projectId, "constraint_analysis", "first result");

        verify(resultRepository).save(any(ProjectStepResult.class));
        assertThat(step.getResult().getContentMarkdown()).isEqualTo("first result");
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.COMPLETED);
    }

    @Test
    @DisplayName("기존 결과물을 수정한다.")
    void saveStepResultUpdatesResult() {
        Project project = createProject();
        ProjectStep step = startedStep(project);
        ProjectStepResult result = ProjectStepResult.builder().step(step).contentMarkdown("old").build();
        step.complete(result);
        stubStepLookup(project, step);

        service().saveStepResult(userId, projectId, "constraint_analysis", "updated");

        assertThat(result.getContentMarkdown()).isEqualTo("updated");
        verify(resultRepository, never()).save(any());
    }

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private ProjectStepService service() {
        return new ProjectStepService(projectValidator, stepRepository, promptRepository, resultRepository);
    }

    private void stubStepLookup(Project project, ProjectStep step) {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepWithDetails(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

    private Project createProject() {
        return Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of("topic", "테스트 주제")).build();
    }

    private ProjectStep createStep(Project project) {
        return ProjectStep.builder().project(project).promptTemplate(promptTemplate)
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
    }

    private ProjectStep startedStep(Project project) {
        ProjectStep step = createStep(project);
        step.start(ProjectStepPrompt.builder().step(step).providedPromptSnapshot("prompt").build());
        return step;
    }
}
