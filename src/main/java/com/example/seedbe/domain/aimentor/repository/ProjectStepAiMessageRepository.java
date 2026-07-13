package com.example.seedbe.domain.aimentor.repository;

import com.example.seedbe.domain.aimentor.entity.ProjectStepAiMessage;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.project.entity.ProjectStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProjectStepAiMessageRepository extends JpaRepository<ProjectStepAiMessage, UUID> {
    @Query("""
            SELECT message
            FROM ProjectStepAiMessage message
            WHERE message.step = :step
              AND EXISTS (
                  SELECT assistant.aiMessageId
                  FROM ProjectStepAiMessage assistant
                  WHERE assistant.step = message.step
                    AND assistant.turnId = message.turnId
                    AND assistant.sender = :assistantSender
              )
            ORDER BY message.createdAt ASC
            """)
    List<ProjectStepAiMessage> findCompletedByStepOrderByCreatedAtAsc(
            @Param("step") ProjectStep step,
            @Param("assistantSender") AiMessageSender assistantSender
    );

    @Query("""
            SELECT message
            FROM ProjectStepAiMessage message
            WHERE message.step = :step
              AND EXISTS (
                  SELECT assistant.aiMessageId
                  FROM ProjectStepAiMessage assistant
                  WHERE assistant.step = message.step
                    AND assistant.turnId = message.turnId
                    AND assistant.sender = :assistantSender
              )
            ORDER BY message.createdAt DESC
            """)
    List<ProjectStepAiMessage> findRecentCompletedByStep(
            @Param("step") ProjectStep step,
            @Param("assistantSender") AiMessageSender assistantSender,
            Pageable pageable
    );

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

    void deleteAllByTurnId(UUID turnId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM project_step_ai_messages user_message
            WHERE user_message.sender = :userSender
              AND user_message.created_at < CURRENT_TIMESTAMP - INTERVAL '2 minutes'
              AND NOT EXISTS (
                  SELECT 1
                  FROM project_step_ai_messages assistant
                  WHERE assistant.step_id = user_message.step_id
                    AND assistant.turn_id = user_message.turn_id
                    AND assistant.sender = :assistantSender
              )
            """, nativeQuery = true)
    int deleteStaleIncompleteTurns(
            @Param("userSender") String userSender,
            @Param("assistantSender") String assistantSender
    );
}
