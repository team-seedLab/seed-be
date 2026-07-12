package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.entity.ProjectStepResult;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.project.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStepServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepResultRepository resultRepository;

    @Test
    void savesStepResultWhenPromptExists() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
        ProjectStep step = ProjectStep.builder().project(project).promptTemplate(org.mockito.Mockito.mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepWithDetails(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(ProjectStepPrompt.builder()
                .step(step).providedPromptSnapshot("provided").build()));

        new ProjectStepService(projectValidator, stepRepository, promptRepository, resultRepository)
                .saveStepResult(userId, projectId, "constraint_analysis", "result");

        verify(resultRepository).save(any(ProjectStepResult.class));
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.COMPLETED);
    }
}
