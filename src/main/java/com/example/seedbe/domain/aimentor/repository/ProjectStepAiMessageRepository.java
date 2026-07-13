package com.example.seedbe.domain.aimentor.repository;

import com.example.seedbe.domain.aimentor.entity.ProjectStepAiMessage;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.project.entity.ProjectStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectStepAiMessageRepository extends JpaRepository<ProjectStepAiMessage, UUID> {
    List<ProjectStepAiMessage> findByStepOrderByCreatedAtAsc(ProjectStep step);

    List<ProjectStepAiMessage> findTop10ByStepOrderByCreatedAtDesc(ProjectStep step);

    long countByStepAndSender(ProjectStep step, AiMessageSender sender);
}
