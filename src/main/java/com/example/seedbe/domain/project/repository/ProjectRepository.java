package com.example.seedbe.domain.project.repository;


import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
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

    Optional<Project> findByProjectIdAndUserUserId(UUID projectId, UUID userId);
}
