package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStepLog;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectStepLogRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.PromptTemplateRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStepServiceTest {

    @Mock
    private ProjectValidator projectValidator;

    @Mock
    private PromptTemplateRepository templateRepository;

    @Mock
    private ProjectStepLogRepository stepLogRepository;

    @Test
    @DisplayName("이미 발급된 단계는 active template을 다시 조회하지 않고 기존 log를 반환한다.")
    void createAndSavePromptReturnsExistingLogWithoutTemplateLookup() {
        ProjectStepService projectStepService = createProjectStepService();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        PromptTemplate promptTemplate = mock(PromptTemplate.class);
        ProjectStepLog existingLog = createStepLog(project, promptTemplate, "prompt", "result");

        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepLogRepository.findByProjectAndRoadmapStepWithPromptTemplate(
                project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(existingLog));
        when(promptTemplate.getFormatPrompt()).thenReturn("format");

        ProjectPromptStepResponse response = projectStepService.createAndSavePrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("prompt");
        assertThat(response.formatPrompt()).isEqualTo("format");
        assertThat(response.userSubmittedResult()).isEqualTo("result");
        verify(templateRepository, never()).findByRoadmapTypeAndRoadmapStepAndIsActiveTrue(any(), any());
        verify(stepLogRepository, never()).saveAndFlush(any(ProjectStepLog.class));
    }

    @Test
    @DisplayName("동시 요청으로 step log unique 충돌이 발생하면 기존 log를 재조회해 반환한다.")
    void createAndSavePromptReturnsConcurrentLogWhenDuplicateInsertOccurs() {
        ProjectStepService projectStepService = createProjectStepService();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        PromptTemplate promptTemplate = mock(PromptTemplate.class);
        ProjectStepLog concurrentLog = createStepLog(project, promptTemplate, "concurrent prompt", null);

        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepLogRepository.findByProjectAndRoadmapStepWithPromptTemplate(
                project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentLog));
        when(templateRepository.findByRoadmapTypeAndRoadmapStepAndIsActiveTrue(
                RoadmapType.REPORT, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(promptTemplate));
        when(promptTemplate.getActionPrompt()).thenReturn("[topic]");
        when(promptTemplate.getFormatPrompt()).thenReturn("format");
        when(stepLogRepository.saveAndFlush(any(ProjectStepLog.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate step log"));

        ProjectPromptStepResponse response = projectStepService.createAndSavePrompt(
                userId, projectId, "constraint_analysis");

        assertThat(response.providedPromptSnapshot()).isEqualTo("concurrent prompt");
        assertThat(response.formatPrompt()).isEqualTo("format");
        verify(stepLogRepository).saveAndFlush(any(ProjectStepLog.class));
    }

    private ProjectStepService createProjectStepService() {
        return new ProjectStepService(projectValidator, templateRepository, stepLogRepository);
    }

    private Project createProject() {
        return Project.builder()
                .title("title")
                .roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS)
                .initialContext(Map.of("topic", "테스트 주제"))
                .build();
    }

    private ProjectStepLog createStepLog(
            Project project,
            PromptTemplate promptTemplate,
            String providedPromptSnapshot,
            String userSubmittedResult
    ) {
        return ProjectStepLog.builder()
                .project(project)
                .promptTemplate(promptTemplate)
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS)
                .providedPromptSnapshot(providedPromptSnapshot)
                .userSubmittedResult(userSubmittedResult)
                .build();
    }
}
