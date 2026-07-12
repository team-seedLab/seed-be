package com.example.seedbe.domain.project.repository;


import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.dto.ProjectStatusCountResponse;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    @Query("SELECT p FROM Project p JOIN FETCH p.user WHERE p.projectId = :projectId")
    Optional<Project> findByIdWithUser(@Param("projectId") UUID projectId);

    @Query("""
        SELECT p FROM Project p 
        WHERE p.user.userId = :userId 
        AND (:status IS NULL OR p.status = :status)
    """)
    Page<Project> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") ProjectStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT new com.example.seedbe.domain.project.dto.ProjectStatusCountResponse(
            COUNT(p),
            COALESCE(SUM(CASE WHEN p.status = com.example.seedbe.domain.project.enums.ProjectStatus.IN_PROGRESS THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN p.status = com.example.seedbe.domain.project.enums.ProjectStatus.COMPLETED THEN 1 ELSE 0 END), 0)
        )
        FROM Project p
        WHERE p.user.userId = :userId
    """)
    ProjectStatusCountResponse countByStatusForUser(@Param("userId") UUID userId);
}
