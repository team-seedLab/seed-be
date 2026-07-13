package com.example.seedbe.domain.aimentor.dto;

import com.example.seedbe.domain.aimentor.entity.ProjectStepAiMessage;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiMessageResponse(
        UUID aiMessageId,
        UUID turnId,
        AiMessageSender sender,
        AiMessageType messageType,
        String content,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        LocalDateTime createdAt
) {
    public static AiMessageResponse from(ProjectStepAiMessage message) {
        return new AiMessageResponse(
                message.getAiMessageId(),
                message.getTurnId(),
                message.getSender(),
                message.getMessageType(),
                message.getContent(),
                message.getInputTokens(),
                message.getOutputTokens(),
                message.getTotalTokens(),
                message.getCreatedAt()
        );
    }
}
