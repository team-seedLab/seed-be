package com.example.seedbe.domain.prompt.repository;

import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {
    Optional<PromptTemplate> findByRoadmapTypeAndRoadmapStepAndIsActiveTrue(RoadmapType roadmapType, RoadmapStep roadmapStep);

}
