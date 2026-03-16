package com.example.seedbe.domain.project.repository;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStepLog;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectStepLogRepository extends JpaRepository<ProjectStepLog, UUID> {
    Optional<ProjectStepLog> findByProjectAndRoadmapStep(Project project, RoadmapStep requestedStep);
}
