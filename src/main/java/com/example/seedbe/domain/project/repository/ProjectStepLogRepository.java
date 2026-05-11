package com.example.seedbe.domain.project.repository;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStepLog;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStepLogRepository extends JpaRepository<ProjectStepLog, UUID> {
    Optional<ProjectStepLog> findByProjectAndRoadmapStep(Project project, RoadmapStep requestedStep);

    @Query("""
        SELECT sl
        FROM ProjectStepLog sl
        JOIN FETCH sl.promptTemplate
        WHERE sl.project = :project
        AND sl.roadmapStep = :roadmapStep
    """)
    Optional<ProjectStepLog> findByProjectAndRoadmapStepWithPromptTemplate(
            @Param("project") Project project,
            @Param("roadmapStep") RoadmapStep roadmapStep
    );

    @Query("""
        SELECT sl
        FROM ProjectStepLog sl
        JOIN FETCH sl.promptTemplate
        WHERE sl.project = :project
        ORDER BY sl.createdAt ASC
    """)
    List<ProjectStepLog> findByProjectWithPromptTemplateOrderByCreatedAtAsc(@Param("project") Project project);
}
