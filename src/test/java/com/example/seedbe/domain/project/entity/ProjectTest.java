package com.example.seedbe.domain.project.entity;

import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProjectTest {
    @Test
    void completesProjectAndRecordsCompletionTime() {
        Project project = Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
        ProjectStep lastStep = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.REPORT_REVISION).stepOrder(4).build();
        lastStep.complete();

        project.complete(lastStep);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(project.getCompletedAt()).isNotNull();
    }

    @Test
    void cannotCompleteWithNonLastStep() {
        Project project = Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
        ProjectStep step = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
        step.complete();

        assertThatThrownBy(() -> project.complete(step))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_LAST_STEP);
    }

    @Test
    void repeatedCompletionKeepsInitialCompletionTime() {
        Project project = Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.COMPLETED).initialContext(Map.of()).build();
        LocalDateTime initialCompletionTime = LocalDateTime.of(2026, 7, 19, 10, 0);
        ReflectionTestUtils.setField(project, "completedAt", initialCompletionTime);
        ProjectStep lastStep = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.REPORT_REVISION).stepOrder(4).build();

        project.complete(lastStep);

        assertThat(project.getCompletedAt()).isEqualTo(initialCompletionTime);
    }
}
