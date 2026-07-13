package com.example.seedbe.domain.aimentor.entity;

import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.domain.project.entity.ProjectStep;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_step_ai_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectStepAiMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ai_message_id", nullable = false, updatable = false)
    private UUID aiMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private ProjectStep step;

    @Column(name = "turn_id", nullable = false, updatable = false)
    private UUID turnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 30)
    private AiMessageSender sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 50)
    private AiMessageType messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ProjectStepAiMessage(ProjectStep step, UUID turnId, AiMessageSender sender,
                                AiMessageType messageType, String content, Integer inputTokens,
                                Integer outputTokens, Integer totalTokens) {
        this.step = step;
        this.turnId = turnId;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
    }
}
