package com.example.seedbe.domain.mentor.repository;

import com.example.seedbe.domain.mentor.entity.ProjectReview;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectReviewRepository extends JpaRepository<ProjectReview, UUID> {
    Optional<ProjectReview> findByProjectAndMentor(Project project, User mentor);

    @Query("""
        SELECT pr FROM ProjectReview pr
        WHERE pr.mentor.userId = :mentorId
          AND pr.project IN :projects
    """)
    List<ProjectReview> findByMentorIdAndProjects(
            @Param("mentorId") UUID mentorId,
            @Param("projects") List<Project> projects
    );
}
