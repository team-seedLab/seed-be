package com.example.seedbe.domain.selfcheck.service;

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
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.selfcheck.dto.ProjectStepSelfCheckResponse;
import com.example.seedbe.domain.selfcheck.dto.SelfCheckItemRequest;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import com.example.seedbe.domain.selfcheck.repository.ProjectStepSelfCheckRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
class ProjectStepSelfCheckServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepResultRepository resultRepository;
    @Mock private ProjectStepSelfCheckRepository selfCheckRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void createsSelfCheckAndCompletesStep() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubSaveContext(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.of(savedResult(step)));
        when(selfCheckRepository.findByStep(step)).thenReturn(Optional.empty());
        when(selfCheckRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectStepSelfCheckResponse response = service().saveSelfCheck(
                userId, projectId, "constraint_analysis", validItems("처음 작성한 충분한 길이의 답변입니다."));

        assertThat(response.checkItems()).hasSize(3)
                .allSatisfy(item -> assertThat(item.answer()).contains("충분한 길이"));
        assertThat(step.getStatus()).isEqualTo(ProjectStepStatus.COMPLETED);
        assertThat(step.getCompletedAt()).isNotNull();
        verify(selfCheckRepository).saveAndFlush(any(ProjectStepSelfCheck.class));
    }

    @Test
    void overwritesExistingSelfCheck() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepSelfCheck existing = ProjectStepSelfCheck.builder()
                .step(step)
                .checkItems(List.of(new com.example.seedbe.domain.selfcheck.model.SelfCheckItem(
                        "understanding", "질문", "기존 답변은 충분히 길게 작성됨")))
                .build();
        stubSaveContext(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.of(savedResult(step)));
        when(selfCheckRepository.findByStep(step)).thenReturn(Optional.of(existing));
        when(selfCheckRepository.saveAndFlush(existing)).thenReturn(existing);

        service().saveSelfCheck(userId, projectId, "constraint_analysis",
                validItems("수정한 답변도 충분한 길이로 작성했습니다."));

        assertThat(existing.getCheckItems().getFirst().answer()).startsWith("수정한 답변");
    }

    @Test
    void getsSavedSelfCheck() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepSelfCheck saved = ProjectStepSelfCheck.builder()
                .step(step)
                .checkItems(List.of(new com.example.seedbe.domain.selfcheck.model.SelfCheckItem(
                        "understanding", "무엇을 이해했나요?", "핵심 내용을 충분하게 이해했습니다.")))
                .build();
        stubReadContext(project, step);
        when(selfCheckRepository.findByStep(step)).thenReturn(Optional.of(saved));

        assertThat(service().getSelfCheck(userId, projectId, "constraint_analysis").checkItems())
                .hasSize(1);
    }

    @Test
    void returnsPredefinedQuestionsBeforeSubmission() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubReadContext(project, step);
        when(selfCheckRepository.findByStep(step)).thenReturn(Optional.empty());

        ProjectStepSelfCheckResponse response = service().getSelfCheck(
                userId, projectId, "constraint_analysis");

        assertThat(response.selfCheckId()).isNull();
        assertThat(response.checkItems()).extracting("key")
                .containsExactly("core_understanding", "result_application", "uncertainty_review");
        assertThat(response.checkItems()).extracting("answer").containsOnlyNulls();
    }

    @Test
    void rejectsAnswerShorterThanTenCodePoints() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubSaveContext(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.of(savedResult(step)));

        assertThatThrownBy(() -> service().saveSelfCheck(
                userId, projectId, "constraint_analysis", validItems("짧은 답변")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.SELF_CHECK_ANSWER_TOO_SHORT);
        verify(selfCheckRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSelfCheckBeforeResultIsSaved() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubSaveContext(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().saveSelfCheck(
                userId, projectId, "constraint_analysis", validItems("충분한 길이로 작성한 답변입니다.")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.GENERATED_RESULT_NOT_FOUND);
    }

    @Test
    void rejectsUnknownQuestionKey() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubSaveContext(project, step);
        when(promptRepository.existsByStep(step)).thenReturn(true);
        when(resultRepository.findByStep(step)).thenReturn(Optional.of(savedResult(step)));
        List<SelfCheckItemRequest> items = List.of(
                new SelfCheckItemRequest("unknown", "충분한 길이로 작성한 첫 번째 답변입니다."),
                new SelfCheckItemRequest("result_application", "충분한 길이로 작성한 두 번째 답변입니다."),
                new SelfCheckItemRequest("uncertainty_review", "충분한 길이로 작성한 세 번째 답변입니다.")
        );

        assertThatThrownBy(() -> service().saveSelfCheck(
                userId, projectId, "constraint_analysis", items))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_SELF_CHECK_ITEMS);
    }

    @Test
    void rejectsAccessToAnotherUsersProject() {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId))
                .thenThrow(new BusinessException(ErrorType.FORBIDDEN_ACCESS));

        assertThatThrownBy(() -> service().getSelfCheck(userId, projectId, "constraint_analysis"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.FORBIDDEN_ACCESS);
        verify(stepRepository, never()).findByProjectAndRoadmapStep(any(), any());
    }

    private ProjectStepSelfCheckService service() {
        return new ProjectStepSelfCheckService(projectValidator, stepRepository, promptRepository,
                resultRepository, selfCheckRepository);
    }

    private List<SelfCheckItemRequest> validItems(String answer) {
        return List.of(
                new SelfCheckItemRequest("core_understanding", answer),
                new SelfCheckItemRequest("result_application", answer),
                new SelfCheckItemRequest("uncertainty_review", answer)
        );
    }

    private Project createProject() {
        return Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
    }

    private ProjectStep createStep(Project project) {
        return ProjectStep.builder().project(project).promptTemplate(org.mockito.Mockito.mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
    }

    private ProjectStepResult savedResult(ProjectStep step) {
        return ProjectStepResult.builder().step(step).contentMarkdown("# 저장된 결과물").build();
    }

    private void stubSaveContext(Project project, ProjectStep step) {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStepForUpdate(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

    private void stubReadContext(Project project, ProjectStep step) {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectAndRoadmapStep(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }
}
