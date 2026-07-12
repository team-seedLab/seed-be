package com.example.seedbe.domain.project.repository;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStepRepository extends JpaRepository<ProjectStep, UUID> {
    Optional<ProjectStep> findByProjectAndRoadmapStep(Project project, RoadmapStep roadmapStep);

    @Query("""
        SELECT ps FROM ProjectStep ps
        JOIN FETCH ps.promptTemplate
        JOIN FETCH ps.prompt
        LEFT JOIN FETCH ps.result
        WHERE ps.project = :project
        ORDER BY ps.stepOrder ASC
    """)
    List<ProjectStep> findStartedStepsWithDetailsOrderByStepOrder(@Param("project") Project project);

    @Query("""
        SELECT ps FROM ProjectStep ps
        JOIN FETCH ps.promptTemplate
        LEFT JOIN FETCH ps.prompt
        LEFT JOIN FETCH ps.result
        WHERE ps.project = :project AND ps.roadmapStep = :roadmapStep
    """)
    Optional<ProjectStep> findByProjectAndRoadmapStepWithDetails(
            @Param("project") Project project,
            @Param("roadmapStep") RoadmapStep roadmapStep
    );
}
