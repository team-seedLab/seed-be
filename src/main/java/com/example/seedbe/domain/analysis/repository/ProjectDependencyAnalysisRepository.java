package com.example.seedbe.domain.analysis.repository;

import com.example.seedbe.domain.analysis.entity.ProjectDependencyAnalysis;
import com.example.seedbe.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectDependencyAnalysisRepository extends JpaRepository<ProjectDependencyAnalysis, UUID> {
    Optional<ProjectDependencyAnalysis> findByProject(Project project);
}
