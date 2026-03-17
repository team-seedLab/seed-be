package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectCreateRequest;
import com.example.seedbe.domain.project.dto.ProjectDetailResponse;
import com.example.seedbe.domain.project.dto.ProjectListResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStepLog;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.domain.project.repository.ProjectStepLogRepository;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectValidator projectValidator;
    private final ProjectRepository projectRepository;
    private final ProjectStepLogRepository stepLogRepository;
    private final PdfService pdfService;
    private final AIService aiService;

    public Page<ProjectListResponse> getProjects(UUID userId, Pageable pageable) {
        Page<Project> projectPage = projectRepository.findAllByUser_UserId(userId, pageable);
        return projectPage.map(ProjectListResponse::from);
    }

    public ProjectDetailResponse getProjectDetails(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        return ProjectDetailResponse.from(project);
    }

    @Transactional
    public ProjectDetailResponse createProject(User user, ProjectCreateRequest projectCreateRequest) {
        // pdf 텍스트화
        String finalPdfText = pdfService.combineTexts(projectCreateRequest.files());

        if (finalPdfText.isEmpty() && projectCreateRequest.userIntent().isBlank()) {
            throw new BusinessException(ErrorType.NO_CONTENT_TO_ANALYZE);
        }

        // ai를 활용한 프롬프트 변수추출
        Map<String, Object> extractedVariables = aiService.analyzeToJSON(finalPdfText, projectCreateRequest.userIntent(), projectCreateRequest.roadmapType());

        Project project = Project.builder()
                .user(user)
                .title(projectCreateRequest.title())
                .roadmapType(projectCreateRequest.roadmapType())
                .initialContext(extractedVariables)
                .status(ProjectStatus.IN_PROGRESS)
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectDetailResponse.from(savedProject);
    }

    @Transactional
    public void deleteProject(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        projectRepository.delete(project);
    }

    @Transactional
    public void completeProject(UUID userId, UUID projectId) {
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);

        ProjectStepLog lastStepLog = stepLogRepository.findByProjectAndRoadmapStep(
                        project, project.getRoadmapType().getLastStep())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));

        project.complete(lastStepLog);
    }
}
