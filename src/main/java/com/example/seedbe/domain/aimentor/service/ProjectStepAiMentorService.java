package com.example.seedbe.domain.aimentor.service;

import com.example.seedbe.domain.aimentor.client.AiMentorClient;
import com.example.seedbe.domain.aimentor.component.ProjectContextRetriever;
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
import com.example.seedbe.domain.user.repository.UserRepository;
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
    private static final long MAX_QUESTION_COUNT_PER_STEP = 10;
    private static final long MAX_QUESTION_COUNT_PER_DAY = 40;

    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepAiMessageRepository messageRepository;
    private final AiMentorClient aiMentorClient;
    private final ProjectContextRetriever contextRetriever;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AiMessageResponse> getMessages(UUID userId, UUID projectId, String stepCode) {
        ValidatedContext context = getContext(userId, projectId, stepCode, false);
        return messageRepository.findByStepOrderByCreatedAtAsc(context.step()).stream()
                .map(AiMessageResponse::from)
                .toList();
    }

    @Transactional
    public List<AiMessageResponse> createMessage(UUID userId, UUID projectId, String stepCode,
                                                 AiMessageType messageType, String question) {
        lockUser(userId);
        ValidatedContext validatedContext = getContext(userId, projectId, stepCode, true);
        Project project = validatedContext.project();
        ProjectStep step = validatedContext.step();
        validateQuestionCount(userId, step);

        ProjectStepPrompt prompt = promptRepository.findByStep(step)
                .orElseThrow(() -> new BusinessException(ErrorType.GENERATED_PROMPT_NOT_FOUND));
        validateMessageType(messageType, prompt);

        List<AiMentorClient.ConversationMessage> recentMessages = getRecentMessages(step);
        String finalPrompt = prompt.getEditedPrompt() == null
                ? prompt.getProvidedPromptSnapshot()
                : prompt.getEditedPrompt();
        String retrievalQuery = String.join("\n",
                question,
                step.getRoadmapStep().getDescription(),
                finalPrompt,
                valueOrEmpty(project.getKeyFocus()),
                valueOrEmpty(project.getRequiredElements()));
        String relevantSource = contextRetriever.retrieve(project.getInitialContext(), retrievalQuery);
        AiMentorClient.AiMentorContext context = new AiMentorClient.AiMentorContext(
                finalPrompt,
                relevantSource,
                project.getDesiredOutcome(),
                project.getKeyFocus(),
                project.getRequiredElements(),
                recentMessages);

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

    private ValidatedContext getContext(UUID userId, UUID projectId, String stepCode, boolean forUpdate) {
        RoadmapStep roadmapStep = RoadmapStep.fromStepCode(stepCode);
        Project project = projectValidator.getProjectWithOwnershipCheck(userId, projectId);
        project.getRoadmapType().validateStep(roadmapStep);

        if (forUpdate) {
            ProjectStep step = stepRepository.findByProjectAndRoadmapStepForUpdate(project, roadmapStep)
                    .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
            return new ValidatedContext(project, step);
        }
        ProjectStep step = stepRepository.findByProjectAndRoadmapStep(project, roadmapStep)
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        return new ValidatedContext(project, step);
    }

    private void lockUser(UUID userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));
    }

    private void validateQuestionCount(UUID userId, ProjectStep step) {
        long questionCount = messageRepository.countByStepAndSender(step, AiMessageSender.USER);
        if (questionCount >= MAX_QUESTION_COUNT_PER_STEP) {
            throw new BusinessException(ErrorType.AI_MENTOR_QUESTION_LIMIT_EXCEEDED);
        }
        if (messageRepository.countUserQuestionsToday(userId) >= MAX_QUESTION_COUNT_PER_DAY) {
            throw new BusinessException(ErrorType.AI_MENTOR_DAILY_LIMIT_EXCEEDED);
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ValidatedContext(Project project, ProjectStep step) {
    }
}
