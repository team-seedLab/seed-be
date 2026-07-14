package com.example.seedbe.domain.prompt.repository;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStepPromptRepository extends JpaRepository<ProjectStepPrompt, UUID> {
    Optional<ProjectStepPrompt> findByStep(ProjectStep step);
    boolean existsByStep(ProjectStep step);
    List<ProjectStepPrompt> findByStepIn(List<ProjectStep> steps);
}
