package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.dto.ProjectListResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.repository.ProjectRepository;
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
}
