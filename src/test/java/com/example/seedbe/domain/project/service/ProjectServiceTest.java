package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.analysis.service.ProjectDependencyAnalysisService;
import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.component.ProjectContext;
import com.example.seedbe.domain.project.dto.ProjectCreateRequest;
import com.example.seedbe.domain.project.dto.ProjectDetailResponse;
import com.example.seedbe.domain.project.dto.ProjectResponse;
import com.example.seedbe.domain.project.dto.ProjectListResponse;
import com.example.seedbe.domain.project.dto.ProjectStatusCountResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.PromptTemplateRepository;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.selfcheck.repository.ProjectStepSelfCheckRepository;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private PromptTemplateRepository templateRepository;
    @Mock private ProjectStepResultRepository resultRepository;
    @Mock private ProjectStepSelfCheckRepository selfCheckRepository;
    @Mock private ProjectDependencyAnalysisService dependencyAnalysisService;
    @Mock private PdfService pdfService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private TransactionStatus transactionStatus;

    @Test
    @DisplayName("PDF와 사용자 입력이 모두 비어 있으면 DB를 호출하지 않는다.")
    void createProjectRejectsEmptyInput() {
        ProjectCreateRequest request = new ProjectCreateRequest("title", RoadmapType.REPORT, "", "", "", null);
        when(pdfService.parse(request.files())).thenReturn(PdfService.PdfParseResult.empty());

        assertThatThrownBy(() -> service().createProject(createUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.NO_CONTENT_TO_ANALYZE);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("프로젝트 생성 transaction에서 로드맵 전체 단계를 순서대로 생성한다.")
    void createProjectCreatesRoadmapSteps() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "title", RoadmapType.REPORT, "원하는 결과", "핵심 관점", "필수 요소", null);
        when(pdfService.parse(request.files())).thenReturn(new PdfService.PdfParseResult("PDF 텍스트"));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        List<PromptTemplate> templates = new java.util.ArrayList<>();
        for (int i = 0; i < RoadmapType.REPORT.getValidSteps().size(); i++) {
            PromptTemplate template = mock(PromptTemplate.class);
            when(template.getStepOrder()).thenReturn(i + 1);
            when(template.getRoadmapStep()).thenReturn(RoadmapType.REPORT.getValidSteps().get(i));
            templates.add(template);
        }
        when(templateRepository.findByRoadmapTypeAndIsActiveTrueOrderByStepOrderAsc(RoadmapType.REPORT))
                .thenReturn(templates);

        ProjectResponse response = service().createProject(createUser(), request);

        ArgumentCaptor<List<ProjectStep>> captor = ArgumentCaptor.forClass(List.class);
        verify(stepRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ProjectStep::getRoadmapStep)
                .containsExactlyElementsOf(RoadmapType.REPORT.getValidSteps());
        assertThat(captor.getValue()).extracting(ProjectStep::getStepOrder)
                .containsExactly(1, 2, 3, 4);
        assertThat(response.title()).isEqualTo("title");
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getDesiredOutcome()).isEqualTo("원하는 결과");
        assertThat(projectCaptor.getValue().getKeyFocus()).isEqualTo("핵심 관점");
        assertThat(projectCaptor.getValue().getRequiredElements()).isEqualTo("필수 요소");
        assertThat(projectCaptor.getValue().getInitialContext())
                .containsEntry(ProjectContext.VERSION_KEY, ProjectContext.RAW_DOCUMENT_VERSION)
                .containsEntry(ProjectContext.SOURCE_TEXT_KEY, "PDF 텍스트");
    }

    @Test
    @DisplayName("로드맵 template 순서가 중복되면 프로젝트와 단계를 저장하지 않는다.")
    void createProjectRejectsInvalidTemplateOrder() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "title", RoadmapType.REPORT, "원하는 결과", "핵심 관점", "필수 요소", null);
        when(pdfService.parse(request.files())).thenReturn(new PdfService.PdfParseResult("PDF 텍스트"));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PromptTemplate duplicatedOrder = mock(PromptTemplate.class);
        when(duplicatedOrder.getStepOrder()).thenReturn(1);
        when(duplicatedOrder.getRoadmapStep()).thenReturn(RoadmapStep.CONSTRAINT_ANALYSIS);
        when(templateRepository.findByRoadmapTypeAndIsActiveTrueOrderByStepOrderAsc(RoadmapType.REPORT))
                .thenReturn(List.of(duplicatedOrder, duplicatedOrder, duplicatedOrder, duplicatedOrder));

        assertThatThrownBy(() -> service().createProject(createUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_ROADMAP_TEMPLATE);
        verify(stepRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("상세 조회는 prompt와 result 없이 전체 단계 요약을 반환한다.")
    void getProjectDetailsReturnsStepSummaries() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        ProjectStep step = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
        step.complete();
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectOrderByStepOrderAsc(project)).thenReturn(List.of(step));
        ProjectDetailResponse response = service().getProjectDetails(userId, projectId);

        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().getFirst().roadmapStep()).isEqualTo(RoadmapStep.CONSTRAINT_ANALYSIS);
        assertThat(response.completedStepCount()).isEqualTo(1);
        assertThat(response.progressPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("목록은 IN_PROGRESS 단계를 현재 단계로 우선하고 진행률을 계산한다.")
    void getProjectsCalculatesCurrentStepAndProgress() {
        UUID userId = UUID.randomUUID();
        Project project = createProject();
        ProjectStep completed = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
        completed.complete();
        ProjectStep current = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.ARGUMENT_STRUCTURING).stepOrder(2).build();
        current.start();
        ProjectStep pending = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.DRAFT_GENERATION).stepOrder(3).build();
        PageRequest pageable = PageRequest.of(0, 10);
        when(projectRepository.findByUserIdAndStatus(userId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(project), pageable, 1));
        when(stepRepository.findSummariesByProjects(List.of(project)))
                .thenReturn(List.of(completed, current, pending));

        Page<ProjectListResponse> response = service().getProjects(userId, null, pageable);

        ProjectListResponse item = response.getContent().getFirst();
        assertThat(item.currentRoadmapStep()).isEqualTo(RoadmapStep.ARGUMENT_STRUCTURING);
        assertThat(item.currentStepOrder()).isEqualTo(2);
        assertThat(item.completedStepCount()).isEqualTo(1);
        assertThat(item.totalStepCount()).isEqualTo(3);
        assertThat(item.progressPercent()).isEqualTo(33);
    }

    @Test
    @DisplayName("모든 단계가 완료되면 마지막 단계를 현재 단계로 반환한다.")
    void getProjectsUsesLastCompletedStep() {
        UUID userId = UUID.randomUUID();
        Project project = createProject();
        ProjectStep first = completedStep(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        ProjectStep last = completedStep(project, RoadmapStep.ARGUMENT_STRUCTURING, 2);
        PageRequest pageable = PageRequest.of(0, 10);
        when(projectRepository.findByUserIdAndStatus(userId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(project), pageable, 1));
        when(stepRepository.findSummariesByProjects(List.of(project))).thenReturn(List.of(first, last));

        ProjectListResponse item = service().getProjects(userId, null, pageable).getContent().getFirst();

        assertThat(item.currentRoadmapStep()).isEqualTo(RoadmapStep.ARGUMENT_STRUCTURING);
        assertThat(item.currentStepOrder()).isEqualTo(2);
        assertThat(item.progressPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("프로젝트가 없으면 빈 목록과 0 상태 개수를 반환한다.")
    void returnsEmptyProjectsAndZeroCounts() {
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        when(projectRepository.findByUserIdAndStatus(userId, null, pageable)).thenReturn(Page.empty(pageable));
        when(projectRepository.countByStatusForUser(userId))
                .thenReturn(new ProjectStatusCountResponse(0, 0, 0));

        assertThat(service().getProjects(userId, null, pageable)).isEmpty();
        assertThat(service().getProjectStatusCounts(userId).totalCount()).isZero();
        verify(stepRepository, never()).findSummariesByProjects(any());
    }

    @Test
    @DisplayName("필수 단계 중 결과물이 비어 있으면 완료할 수 없다.")
    void completeProjectRejectsEmptyLastResult() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        List<ProjectStep> steps = projectSteps(project);
        when(projectValidator.getProjectWithOwnershipCheckForUpdate(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectOrderByStepOrderAsc(project)).thenReturn(steps);
        when(resultRepository.findByStepIn(steps)).thenReturn(resultsFor(steps.subList(0, 3)));

        assertThatThrownBy(() -> service().completeProject(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.GENERATED_RESULT_NOT_FOUND);
    }

    @Test
    @DisplayName("필수 로드맵 단계가 모두 생성되지 않았으면 완료할 수 없다.")
    void completeProjectRejectsMissingRequiredStep() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        List<ProjectStep> incompleteSteps = projectSteps(project).subList(0, 3);
        when(projectValidator.getProjectWithOwnershipCheckForUpdate(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectOrderByStepOrderAsc(project)).thenReturn(incompleteSteps);

        assertThatThrownBy(() -> service().completeProject(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.STEP_NOT_STARTED);

        verify(resultRepository, never()).findByStepIn(any());
    }

    @Test
    @DisplayName("모든 단계 결과물과 이해 확인이 있으면 분석을 저장하고 프로젝트를 완료한다.")
    void completeProjectWithSavedLastResult() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        List<ProjectStep> steps = projectSteps(project);
        List<ProjectStepResult> results = resultsFor(steps);
        List<ProjectStepSelfCheck> selfChecks = selfChecksFor(steps);
        when(projectValidator.getProjectWithOwnershipCheckForUpdate(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectOrderByStepOrderAsc(project)).thenReturn(steps);
        when(resultRepository.findByStepIn(steps)).thenReturn(results);
        when(selfCheckRepository.findByStepIn(steps)).thenReturn(selfChecks);

        service().completeProject(userId, projectId);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(project.getCompletedAt()).isNotNull();
        verify(dependencyAnalysisService).analyzeAndSave(project, steps);
    }

    @Test
    @DisplayName("필수 단계 중 이해 확인이 없으면 프로젝트를 완료할 수 없다.")
    void completeProjectRejectsMissingSelfCheck() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = createProject();
        List<ProjectStep> steps = projectSteps(project);
        List<ProjectStepResult> results = resultsFor(steps);
        List<ProjectStepSelfCheck> selfChecks = selfChecksFor(steps.subList(0, 3));
        when(projectValidator.getProjectWithOwnershipCheckForUpdate(userId, projectId)).thenReturn(project);
        when(stepRepository.findByProjectOrderByStepOrderAsc(project)).thenReturn(steps);
        when(resultRepository.findByStepIn(steps)).thenReturn(results);
        when(selfCheckRepository.findByStepIn(steps)).thenReturn(selfChecks);

        assertThatThrownBy(() -> service().completeProject(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.SELF_CHECK_NOT_FOUND);
    }

    private ProjectService service() {
        return new ProjectService(projectValidator, projectRepository, stepRepository, templateRepository,
                resultRepository, selfCheckRepository, dependencyAnalysisService, pdfService, transactionTemplate);
    }

    private Project createProject() {
        Project project = Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
        ReflectionTestUtils.setField(project, "projectId", UUID.randomUUID());
        return project;
    }

    private ProjectStep completedStep(Project project, RoadmapStep roadmapStep, int stepOrder) {
        ProjectStep step = ProjectStep.builder().project(project).promptTemplate(mock(PromptTemplate.class))
                .roadmapStep(roadmapStep).stepOrder(stepOrder).build();
        step.complete();
        return step;
    }

    private List<ProjectStep> projectSteps(Project project) {
        return IntStream.range(0, RoadmapType.REPORT.getValidSteps().size())
                .mapToObj(index -> {
                    ProjectStep step = ProjectStep.builder()
                            .project(project)
                            .promptTemplate(mock(PromptTemplate.class))
                            .roadmapStep(RoadmapType.REPORT.getValidSteps().get(index))
                            .stepOrder(index + 1)
                            .build();
                    ReflectionTestUtils.setField(step, "stepId", UUID.randomUUID());
                    return step;
                })
                .toList();
    }

    private List<ProjectStepResult> resultsFor(List<ProjectStep> steps) {
        return steps.stream()
                .map(step -> ProjectStepResult.builder().step(step).contentMarkdown("# 결과물").build())
                .toList();
    }

    private List<ProjectStepSelfCheck> selfChecksFor(List<ProjectStep> steps) {
        return steps.stream()
                .map(step -> {
                    ProjectStepSelfCheck selfCheck = mock(ProjectStepSelfCheck.class);
                    when(selfCheck.getStep()).thenReturn(step);
                    return selfCheck;
                })
                .toList();
    }

    private User createUser() {
        return User.builder().provider("google").providerId("provider-id").email("seed@example.com")
                .nickname("seed").role(Role.ROLE_USER).profileUrl("https://example.com/profile.png").build();
    }
}
