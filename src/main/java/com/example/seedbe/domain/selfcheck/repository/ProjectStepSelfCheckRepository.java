package com.example.seedbe.domain.selfcheck.repository;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectStepSelfCheckRepository extends JpaRepository<ProjectStepSelfCheck, UUID> {
    Optional<ProjectStepSelfCheck> findByStep(ProjectStep step);

    boolean existsByStep(ProjectStep step);
}
