package com.example.seedbe.domain.project.repository;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface ProjectStepRepository extends JpaRepository<ProjectStep, UUID> {
    Optional<ProjectStep> findByProjectAndRoadmapStep(Project project, RoadmapStep roadmapStep);

    @Query("""
        SELECT ps FROM ProjectStep ps
        WHERE ps.project IN :projects
        ORDER BY ps.project.projectId ASC, ps.stepOrder ASC
    """)
    List<ProjectStep> findSummariesByProjects(@Param("projects") List<Project> projects);

    List<ProjectStep> findByProjectOrderByStepOrderAsc(Project project);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ps FROM ProjectStep ps
        JOIN FETCH ps.promptTemplate
        WHERE ps.project = :project AND ps.roadmapStep = :roadmapStep
    """)
    Optional<ProjectStep> findByProjectAndRoadmapStepForUpdate(
            @Param("project") Project project,
            @Param("roadmapStep") RoadmapStep roadmapStep
    );

    @Query("""
        SELECT ps FROM ProjectStep ps
        JOIN FETCH ps.promptTemplate
        LEFT JOIN FETCH ps.result
        WHERE ps.project = :project AND ps.roadmapStep = :roadmapStep
    """)
    Optional<ProjectStep> findByProjectAndRoadmapStepWithDetails(
            @Param("project") Project project,
            @Param("roadmapStep") RoadmapStep roadmapStep
    );
}
