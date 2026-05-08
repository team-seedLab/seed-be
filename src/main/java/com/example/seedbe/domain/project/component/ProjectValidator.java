package com.example.seedbe.domain.project.component;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProjectValidator {
    private final ProjectRepository projectRepository;

    public Project getProjectWithOwnershipCheck(UUID userId, UUID projectId) {
        return projectRepository.findByProjectIdAndUserUserId(projectId, userId)
                .orElseThrow(() -> {
                    if (projectRepository.existsById(projectId)) {
                        return new BusinessException(ErrorType.FORBIDDEN_ACCESS);
                    }
                    return new BusinessException(ErrorType.PROJECT_NOT_FOUND);
                });
    }
}
