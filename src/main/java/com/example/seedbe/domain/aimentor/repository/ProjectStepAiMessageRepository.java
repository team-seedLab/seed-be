package com.example.seedbe.domain.aimentor.repository;

import com.example.seedbe.domain.aimentor.entity.ProjectStepAiMessage;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.project.entity.ProjectStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectStepAiMessageRepository extends JpaRepository<ProjectStepAiMessage, UUID> {
    List<ProjectStepAiMessage> findByStepOrderByCreatedAtAsc(ProjectStep step);

    List<ProjectStepAiMessage> findTop10ByStepOrderByCreatedAtDesc(ProjectStep step);

    long countByStepAndSender(ProjectStep step, AiMessageSender sender);

    @Query(value = """
            SELECT COUNT(*)
            FROM project_step_ai_messages message
            JOIN project_steps step ON step.step_id = message.step_id
            JOIN projects project ON project.project_id = step.project_id
            WHERE project.user_id = :userId
              AND message.sender = 'USER'
              AND message.created_at >= (
                  date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
                  AT TIME ZONE 'Asia/Seoul'
              )
              AND message.created_at < (
                  (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul') + INTERVAL '1 day')
                  AT TIME ZONE 'Asia/Seoul'
              )
            """, nativeQuery = true)
    long countUserQuestionsToday(@Param("userId") UUID userId);
}
