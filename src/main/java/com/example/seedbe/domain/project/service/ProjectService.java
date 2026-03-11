package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.dto.ProjectCreateRequest;
import com.example.seedbe.domain.project.dto.ProjectDetailResponse;
import com.example.seedbe.domain.project.dto.ProjectListResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projectRepository;

    public Page<ProjectListResponse> getProjects(UUID userId, Pageable pageable) {
        Page<Project> projectPage = projectRepository.findAllByUserId(userId, pageable);
        return projectPage.map(ProjectListResponse::from);
    }

    public ProjectDetailResponse getProjectDetails(UUID userId, UUID projectId) {
        Project project = getProjectWithOwnershipCheck(userId, projectId);
        return ProjectDetailResponse.from(project);
    }

    public ProjectDetailResponse createProject(UUID userId, ProjectCreateRequest projectCreateRequest) {
        Project project = Project.builder()
                .userId(userId)
                .title(projectCreateRequest.title())
                .roadmapType(projectCreateRequest.roadmapType())
                .initialContext(projectCreateRequest.initialContext())
                .status(ProjectStatus.IN_PROGRESS)
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectDetailResponse.from(savedProject);
    }

    private Project getProjectWithOwnershipCheck(UUID userId, UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorType.PROJECT_NOT_FOUND));

        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(ErrorType.FORBIDDEN_ACCESS);
        }

        return project;
    }
}
