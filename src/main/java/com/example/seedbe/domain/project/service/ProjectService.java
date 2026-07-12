package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectCreateRequest;
import com.example.seedbe.domain.project.dto.ProjectDetailResponse;
import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.dto.ProjectSummaryResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.PromptTemplateRepository;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectValidator projectValidator;
    private final ProjectRepository projectRepository;
    private final ProjectStepRepository stepRepository;
    private final PromptTemplateRepository templateRepository;
    private final PdfService pdfService;
    private final AIService aiService;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public Page<ProjectSummaryResponse> getProjects(UUID userId, ProjectStatus status, Pageable pageable) {
        Page<Project> projectPage = projectRepository.findByUserIdAndStatus(userId, status, pageable);
        return projectPage.map(ProjectSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetails(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);

        ProjectSummaryResponse summary = ProjectSummaryResponse.from(project);

        List<ProjectStep> steps = stepRepository.findStartedStepsWithDetailsOrderByStepOrder(project);
        List<ProjectPromptStepResponse> stepResponses = steps.stream()
                .map(ProjectPromptStepResponse::from)
                .toList();

        return ProjectDetailResponse.of(summary, stepResponses);
    }

    public ProjectSummaryResponse createProject(User user, ProjectCreateRequest projectCreateRequest) {
        // PDF parsing과 Gemini 호출은 DB transaction 밖에서 먼저 끝낸다.
        PdfService.PdfParseResult pdfParseResult = pdfService.parse(projectCreateRequest.files());
        String desiredOutcome = projectCreateRequest.desiredOutcome();
        String keyFocus = projectCreateRequest.keyFocus();
        String requiredElements = projectCreateRequest.requiredElements();

        if (!pdfParseResult.hasExtractedText() && allBlank(desiredOutcome, keyFocus, requiredElements)) {
            throw new BusinessException(ErrorType.NO_CONTENT_TO_ANALYZE);
        }

        Map<String, Object> extractedVariables = aiService.analyzeToJSON(
                pdfParseResult.text(),
                desiredOutcome,
                keyFocus,
                requiredElements,
                projectCreateRequest.roadmapType()
        );

        // 외부 I/O가 끝난 뒤, 실제 DB write 구간만 짧게 transaction으로 묶는다.
        return transactionTemplate.execute(status -> saveProject(user, projectCreateRequest, extractedVariables));
    }

    private ProjectSummaryResponse saveProject(
            User user,
            ProjectCreateRequest projectCreateRequest,
            Map<String, Object> extractedVariables
    ) {
        Project project = Project.builder()
                .user(user)
                .title(projectCreateRequest.title())
                .roadmapType(projectCreateRequest.roadmapType())
                .initialContext(extractedVariables)
                .desiredOutcome(projectCreateRequest.desiredOutcome())
                .keyFocus(projectCreateRequest.keyFocus())
                .requiredElements(projectCreateRequest.requiredElements())
                .status(ProjectStatus.IN_PROGRESS)
                .build();

        Project savedProject = projectRepository.save(project);
        List<PromptTemplate> templates = templateRepository
                .findByRoadmapTypeAndIsActiveTrueOrderByStepOrderAsc(project.getRoadmapType());
        validateTemplates(project, templates);
        List<ProjectStep> steps = templates.stream()
                .map(template -> ProjectStep.builder()
                        .project(savedProject)
                        .promptTemplate(template)
                        .roadmapStep(template.getRoadmapStep())
                        .stepOrder(template.getStepOrder())
                        .status(ProjectStepStatus.PENDING)
                        .build())
                .toList();
        stepRepository.saveAll(steps);
        return ProjectSummaryResponse.from(savedProject);
    }

    private void validateTemplates(Project project, List<PromptTemplate> templates) {
        var expectedSteps = project.getRoadmapType().getValidSteps();
        boolean valid = templates.size() == expectedSteps.size()
                && IntStream.range(0, templates.size()).allMatch(index -> {
                    PromptTemplate template = templates.get(index);
                    return template.getStepOrder() == index + 1
                            && template.getRoadmapStep() == expectedSteps.get(index);
                });
        if (!valid) {
            throw new BusinessException(ErrorType.INVALID_ROADMAP_TEMPLATE);
        }
    }

    private boolean allBlank(String... values) {
        return java.util.Arrays.stream(values).allMatch(value -> value == null || value.isBlank());
    }

    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public void completeProject(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);

        ProjectStep lastStep = stepRepository.findByProjectAndRoadmapStepWithDetails(
                        project, project.getRoadmapType().getLastStep())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));

        project.complete(lastStep);
    }
}
