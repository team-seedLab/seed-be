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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectStepAiMentorService {
    private static final long MAX_QUESTION_COUNT_PER_STEP = 10;
    private static final long MAX_QUESTION_COUNT_PER_DAY = 40;
    private static final int RECENT_MESSAGE_COUNT = 10;

    private final ProjectValidator projectValidator;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepAiMessageRepository messageRepository;
    private final AiMentorClient aiMentorClient;
    private final ProjectContextRetriever contextRetriever;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public List<AiMessageResponse> getMessages(UUID userId, UUID projectId, String stepCode) {
        ValidatedContext context = getContext(userId, projectId, stepCode, false);
        return messageRepository.findCompletedByStepOrderByCreatedAtAsc(
                        context.step(), AiMessageSender.ASSISTANT).stream()
                .map(AiMessageResponse::from)
                .toList();
    }

    public List<AiMessageResponse> createMessage(UUID userId, UUID projectId, String stepCode,
                                                 AiMessageType messageType, String question) {
        PreparedTurn preparedTurn = transactionTemplate.execute(status ->
                prepareTurn(userId, projectId, stepCode, messageType, question));
        if (preparedTurn == null) {
            throw new BusinessException(ErrorType.INTERNAL_SERVER_ERROR);
        }

        try {
            AiMentorClient.AiMentorContext aiContext = buildAiContext(preparedTurn.contextSource(), question);
            AiMentorClient.AiMentorReply reply = aiMentorClient.ask(
                    aiContext, question, messageType);
            List<AiMessageResponse> completedTurn = transactionTemplate.execute(
                    status -> completeTurn(preparedTurn, reply));
            if (completedTurn == null) {
                throw new BusinessException(ErrorType.INTERNAL_SERVER_ERROR);
            }
            return completedTurn;
        } catch (RuntimeException e) {
            cleanupFailedTurn(preparedTurn.turnId());
            throw e;
        }
    }

    private PreparedTurn prepareTurn(UUID userId, UUID projectId, String stepCode,
                                     AiMessageType messageType, String question) {
        messageRepository.deleteStaleIncompleteTurns(
                AiMessageSender.USER.name(),
                AiMessageSender.ASSISTANT.name());
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
        ContextSource contextSource = new ContextSource(
                finalPrompt,
                project.getInitialContext(),
                project.getDesiredOutcome(),
                project.getKeyFocus(),
                project.getRequiredElements(),
                step.getRoadmapStep().getDescription(),
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
        return new PreparedTurn(
                turnId,
                step.getStepId(),
                messageType,
                AiMessageResponse.from(userMessage),
                contextSource
        );
    }

    private AiMentorClient.AiMentorContext buildAiContext(ContextSource source, String question) {
        ProjectContextRetriever.RetrievalQuery retrievalQuery = new ProjectContextRetriever.RetrievalQuery(
                question,
                source.stepDescription(),
                source.keyFocus(),
                source.requiredElements());
        String relevantSource = contextRetriever.retrieve(source.initialContext(), retrievalQuery);
        return new AiMentorClient.AiMentorContext(
                source.finalPrompt(),
                relevantSource,
                source.desiredOutcome(),
                source.keyFocus(),
                source.requiredElements(),
                source.recentMessages());
    }

    private List<AiMessageResponse> completeTurn(PreparedTurn preparedTurn,
                                                 AiMentorClient.AiMentorReply reply) {
        ProjectStep step = stepRepository.findById(preparedTurn.stepId())
                .orElseThrow(() -> new BusinessException(ErrorType.STEP_NOT_STARTED));
        ProjectStepAiMessage assistantMessage = ProjectStepAiMessage.builder()
                .step(step)
                .turnId(preparedTurn.turnId())
                .sender(AiMessageSender.ASSISTANT)
                .messageType(preparedTurn.messageType())
                .content(reply.content())
                .inputTokens(reply.inputTokens())
                .outputTokens(reply.outputTokens())
                .totalTokens(reply.totalTokens())
                .build();
        messageRepository.saveAndFlush(assistantMessage);

        return List.of(preparedTurn.userMessage(), AiMessageResponse.from(assistantMessage));
    }

    private void cleanupFailedTurn(UUID turnId) {
        try {
            transactionTemplate.execute(status -> {
                messageRepository.deleteAllByTurnId(turnId);
                return null;
            });
        } catch (RuntimeException cleanupException) {
            log.error("Failed to clean up incomplete AI mentor turn: {}", turnId, cleanupException);
        }
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
                messageRepository.findRecentCompletedByStep(
                        step, AiMessageSender.ASSISTANT, PageRequest.of(0, RECENT_MESSAGE_COUNT)));
        Collections.reverse(messages);

        Map<UUID, TurnMessages> turns = new LinkedHashMap<>();
        for (ProjectStepAiMessage message : messages) {
            TurnMessages turn = turns.computeIfAbsent(message.getTurnId(), ignored -> new TurnMessages());
            turn.add(message);
        }

        List<AiMentorClient.ConversationMessage> conversation = new ArrayList<>();
        for (TurnMessages turn : turns.values()) {
            if (turn.userMessage != null && turn.assistantMessage != null) {
                conversation.add(new AiMentorClient.ConversationMessage(
                        AiMessageSender.USER, turn.userMessage.getContent()));
                conversation.add(new AiMentorClient.ConversationMessage(
                        AiMessageSender.ASSISTANT, turn.assistantMessage.getContent()));
            }
        }
        return conversation;
    }

    private record ValidatedContext(Project project, ProjectStep step) {
    }

    private record PreparedTurn(
            UUID turnId,
            UUID stepId,
            AiMessageType messageType,
            AiMessageResponse userMessage,
            ContextSource contextSource
    ) {
    }

    private record ContextSource(
            String finalPrompt,
            Map<String, Object> initialContext,
            String desiredOutcome,
            String keyFocus,
            String requiredElements,
            String stepDescription,
            List<AiMentorClient.ConversationMessage> recentMessages
    ) {
    }

    private static class TurnMessages {
        private ProjectStepAiMessage userMessage;
        private ProjectStepAiMessage assistantMessage;

        private void add(ProjectStepAiMessage message) {
            if (message.getSender() == AiMessageSender.USER) {
                userMessage = message;
            } else if (message.getSender() == AiMessageSender.ASSISTANT) {
                assistantMessage = message;
            }
        }
    }
}
