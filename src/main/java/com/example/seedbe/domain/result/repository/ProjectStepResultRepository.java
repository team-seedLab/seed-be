package com.example.seedbe.domain.result.repository;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStepResultRepository extends JpaRepository<ProjectStepResult, UUID> {
    Optional<ProjectStepResult> findByStep(ProjectStep step);
    List<ProjectStepResult> findByStepIn(List<ProjectStep> steps);
}
