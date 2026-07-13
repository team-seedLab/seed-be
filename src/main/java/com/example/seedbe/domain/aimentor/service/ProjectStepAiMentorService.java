package com.example.seedbe.domain.aimentor.service;

import com.example.seedbe.domain.aimentor.client.AiMentorClient;
import com.example.seedbe.domain.aimentor.dto.AiMessageResponse;
import com.example.seedbe.domain.aimentor.entity.ProjectStepAiMessage;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.domain.aimentor.repository.ProjectStepAiMessageRepository;
import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectStepAiMentorService {
    private static final long MAX_QUESTION_COUNT_PER_STEP = 20;

    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepAiMessageRepository messageRepository;
    private final AiMentorClient aiMentorClient;

    @Transactional(readOnly = true)
    public List<AiMessageResponse> getMessages(UUID userId, UUID projectId, String stepCode) {
        ProjectStep step = getStep(userId, projectId, stepCode, false);
        return messageRepository.findByStepOrderByCreatedAtAsc(step).stream()
                .map(AiMessageResponse::from)
                .toList();
    }

    @Transactional
    public List<AiMessageResponse> createMessage(UUID userId, UUID projectId, String stepCode,
                                                 AiMessageType messageType, String question) {
        ProjectStep step = getStep(userId, projectId, stepCode, true);
        validateQuestionCount(step);

        ProjectStepPrompt prompt = promptRepository.findByStep(step)
                .orElseThrow(() -> new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND));
        validateMessageType(messageType, prompt);

        List<AiMentorClient.ConversationMessage> recentMessages = getRecentMessages(step);
        AiMentorClient.AiMentorContext context = new AiMentorClient.AiMentorContext(
                prompt.getProvidedPromptSnapshot(), prompt.getEditedPrompt(), recentMessages);

        UUID turnId = UUID.randomUUID();
        ProjectStepAiMessage userMessage = ProjectStepAiMessage.builder()
                .step(step)
                .turnId(turnId)
                .sender(AiMessageSender.USER)
                .messageType(messageType)
                .content(question)
                .build();
        messageRepository.saveAndFlush(userMessage);

        AiMentorClient.AiMentorReply reply = aiMentorClient.ask(context, question, messageType);
        ProjectStepAiMessage assistantMessage = ProjectStepAiMessage.builder()
                .step(step)
                .turnId(turnId)
                .sender(AiMessageSender.ASSISTANT)
                .messageType(messageType)
                .content(reply.content())
                .inputTokens(reply.inputTokens())
                .outputTokens(reply.outputTokens())
                .totalTokens(reply.totalTokens())
                .build();
        messageRepository.saveAndFlush(assistantMessage);

        return List.of(AiMessageResponse.from(userMessage), AiMessageResponse.from(assistantMessage));
    }

    private ProjectStep getStep(UUID userId, UUID projectId, String stepCode, boolean forUpdate) {
        RoadmapStep roadmapStep = RoadmapStep.fromStepCode(stepCode);
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        project.getRoadmapType().validateStep(roadmapStep);

        if (forUpdate) {
            return stepRepository.findByProjectAndRoadmapStepForUpdate(project, roadmapStep)
                    .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        }
        return stepRepository.findByProjectAndRoadmapStep(project, roadmapStep)
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
    }

    private void validateQuestionCount(ProjectStep step) {
        long questionCount = messageRepository.countByStepAndSender(step, AiMessageSender.USER);
        if (questionCount >= MAX_QUESTION_COUNT_PER_STEP) {
            throw new BusinessException(ErrorType.AI_MENTOR_QUESTION_LIMIT_EXCEEDED);
        }
    }

    private void validateMessageType(AiMessageType messageType, ProjectStepPrompt prompt) {
        if (messageType == AiMessageType.REASK_WITH_EDITED_PROMPT
                && (prompt.getEditedPrompt() == null || prompt.getEditedPrompt().isBlank())) {
            throw new BusinessException(ErrorType.EDITED_PROMPT_NOT_FOUND);
        }
    }

    private List<AiMentorClient.ConversationMessage> getRecentMessages(ProjectStep step) {
        List<ProjectStepAiMessage> messages = new ArrayList<>(
                messageRepository.findTop10ByStepOrderByCreatedAtDesc(step));
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> new AiMentorClient.ConversationMessage(
                        message.getSender(), message.getContent()))
                .toList();
    }
}
