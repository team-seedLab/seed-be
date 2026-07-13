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
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.repository.UserRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStepAiMentorServiceTest {
    @Mock private ProjectValidator projectValidator;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepAiMessageRepository messageRepository;
    @Mock private AiMentorClient aiMentorClient;
    @Mock private ProjectContextRetriever contextRetriever;
    @Mock private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void savesUserAndAssistantAsOneTurnWithTokenUsage() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = createPrompt(step, "original", "edited");
        stubLockedContext(project, step, prompt);
        when(messageRepository.countByStepAndSender(step, AiMessageSender.USER)).thenReturn(0L);
        when(messageRepository.findTop10ByStepOrderByCreatedAtDesc(step)).thenReturn(List.of());
        when(contextRetriever.retrieve(any(), any())).thenReturn("관련 PDF 내용");
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiMentorClient.ask(any(), any(), any())).thenReturn(
                new AiMentorClient.AiMentorReply("answer", 12, 8, 20));

        List<AiMessageResponse> responses = service().createMessage(
                userId, projectId, "constraint_analysis", AiMessageType.CHAT, "question");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).sender()).isEqualTo(AiMessageSender.USER);
        assertThat(responses.get(1).sender()).isEqualTo(AiMessageSender.ASSISTANT);
        assertThat(responses.get(0).turnId()).isEqualTo(responses.get(1).turnId());
        assertThat(responses.get(1).totalTokens()).isEqualTo(20);
    }

    @Test
    void sendsOnlyTenRecentMessagesInChronologicalOrder() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        ProjectStepPrompt prompt = createPrompt(step, "original", null);
        stubLockedContext(project, step, prompt);
        when(messageRepository.countByStepAndSender(step, AiMessageSender.USER)).thenReturn(3L);
        when(messageRepository.findTop10ByStepOrderByCreatedAtDesc(step)).thenReturn(List.of(
                message(step, AiMessageSender.ASSISTANT, "new"),
                message(step, AiMessageSender.USER, "old")
        ));
        when(contextRetriever.retrieve(any(), any())).thenReturn("관련 PDF 내용");
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiMentorClient.ask(any(), any(), any())).thenReturn(
                new AiMentorClient.AiMentorReply("answer", 1, 1, 2));

        service().createMessage(userId, projectId, "constraint_analysis", AiMessageType.CHAT, "now");

        ArgumentCaptor<AiMentorClient.AiMentorContext> captor =
                ArgumentCaptor.forClass(AiMentorClient.AiMentorContext.class);
        verify(aiMentorClient).ask(captor.capture(), any(), any());
        assertThat(captor.getValue().finalPrompt()).isEqualTo("original");
        assertThat(captor.getValue().relevantSourceContext()).isEqualTo("관련 PDF 내용");
        assertThat(captor.getValue().recentMessages())
                .extracting(AiMentorClient.ConversationMessage::content)
                .containsExactly("old", "new");
    }

    @Test
    void rejectsReaskWithoutEditedPrompt() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubLockedContext(project, step, createPrompt(step, "original", null));
        when(messageRepository.countByStepAndSender(step, AiMessageSender.USER)).thenReturn(0L);

        assertThatThrownBy(() -> service().createMessage(
                userId, projectId, "constraint_analysis",
                AiMessageType.REASK_WITH_EDITED_PROMPT, "again"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.EDITED_PROMPT_NOT_FOUND);
        verify(aiMentorClient, never()).ask(any(), any(), any());
    }

    @Test
    void rejectsMoreThanTenQuestionsPerStep() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubLockedStep(project, step);
        when(messageRepository.countByStepAndSender(step, AiMessageSender.USER)).thenReturn(10L);

        assertThatThrownBy(() -> service().createMessage(
                userId, projectId, "constraint_analysis", AiMessageType.CHAT, "question"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_QUESTION_LIMIT_EXCEEDED);
        verify(promptRepository, never()).findByStep(any());
    }

    @Test
    void rejectsMoreThanFortyQuestionsPerDayAcrossProjects() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubLockedStep(project, step);
        when(messageRepository.countByStepAndSender(step, AiMessageSender.USER)).thenReturn(3L);
        when(messageRepository.countUserQuestionsToday(userId)).thenReturn(40L);

        assertThatThrownBy(() -> service().createMessage(
                userId, projectId, "constraint_analysis", AiMessageType.CHAT, "question"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_DAILY_LIMIT_EXCEEDED);
        verify(promptRepository, never()).findByStep(any());
    }

    @Test
    void propagatesAiFailureBeforeAssistantIsSaved() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubLockedContext(project, step, createPrompt(step, "original", null));
        when(messageRepository.countByStepAndSender(step, AiMessageSender.USER)).thenReturn(0L);
        when(messageRepository.findTop10ByStepOrderByCreatedAtDesc(step)).thenReturn(List.of());
        when(contextRetriever.retrieve(any(), any())).thenReturn("관련 PDF 내용");
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiMentorClient.ask(any(), any(), any()))
                .thenThrow(new BusinessException(ErrorType.AI_MENTOR_SERVER_ERROR));

        assertThatThrownBy(() -> service().createMessage(
                userId, projectId, "constraint_analysis", AiMessageType.CHAT, "question"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_SERVER_ERROR);

        ArgumentCaptor<ProjectStepAiMessage> captor = ArgumentCaptor.forClass(ProjectStepAiMessage.class);
        verify(messageRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSender()).isEqualTo(AiMessageSender.USER);
    }

    @Test
    void getsMessagesInRepositoryOrderAfterOwnershipCheck() {
        Project project = createProject();
        ProjectStep step = createStep(project);
        stubStep(project, step);
        when(messageRepository.findByStepOrderByCreatedAtAsc(step)).thenReturn(List.of(
                message(step, AiMessageSender.USER, "question"),
                message(step, AiMessageSender.ASSISTANT, "answer")
        ));

        assertThat(service().getMessages(userId, projectId, "constraint_analysis"))
                .extracting(AiMessageResponse::content)
                .containsExactly("question", "answer");
    }

    @Test
    void rejectsAccessToAnotherUsersProject() {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId))
                .thenThrow(new BusinessException(ErrorType.FORBIDDEN_ACCESS));

        assertThatThrownBy(() -> service().getMessages(
                userId, projectId, "constraint_analysis"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.FORBIDDEN_ACCESS);
        verify(stepRepository, never()).findByProjectAndRoadmapStep(any(), any());
        verify(messageRepository, never()).findByStepOrderByCreatedAtAsc(any());
    }

    private ProjectStepAiMentorService service() {
        return new ProjectStepAiMentorService(projectValidator, stepRepository, promptRepository,
                messageRepository, aiMentorClient, contextRetriever, userRepository);
    }

    private Project createProject() {
        return Project.builder().title("title").roadmapType(RoadmapType.REPORT)
                .status(ProjectStatus.IN_PROGRESS).initialContext(Map.of()).build();
    }

    private ProjectStep createStep(Project project) {
        return ProjectStep.builder().project(project).promptTemplate(org.mockito.Mockito.mock(PromptTemplate.class))
                .roadmapStep(RoadmapStep.CONSTRAINT_ANALYSIS).stepOrder(1).build();
    }

    private ProjectStepPrompt createPrompt(ProjectStep step, String provided, String edited) {
        return ProjectStepPrompt.builder().step(step).providedPromptSnapshot(provided)
                .editedPrompt(edited).build();
    }

    private ProjectStepAiMessage message(ProjectStep step, AiMessageSender sender, String content) {
        return ProjectStepAiMessage.builder().step(step).turnId(UUID.randomUUID()).sender(sender)
                .messageType(AiMessageType.CHAT).content(content).build();
    }

    private void stubProject(Project project) {
        when(projectValidator.getProjectWithOwnershipCheck(userId, projectId)).thenReturn(project);
    }

    private void stubLockedStep(Project project, ProjectStep step) {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(org.mockito.Mockito.mock(User.class)));
        stubProject(project);
        when(stepRepository.findByProjectAndRoadmapStepForUpdate(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }

    private void stubLockedContext(Project project, ProjectStep step, ProjectStepPrompt prompt) {
        stubLockedStep(project, step);
        when(promptRepository.findByStep(step)).thenReturn(Optional.of(prompt));
    }

    private void stubStep(Project project, ProjectStep step) {
        stubProject(project);
        when(stepRepository.findByProjectAndRoadmapStep(project, RoadmapStep.CONSTRAINT_ANALYSIS))
                .thenReturn(Optional.of(step));
    }
}
