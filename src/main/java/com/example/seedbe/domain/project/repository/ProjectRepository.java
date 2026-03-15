package com.example.seedbe.domain.project.repository;


import com.example.seedbe.domain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findAllByUser_UserId(UUID userId, Pageable pageable);
}
