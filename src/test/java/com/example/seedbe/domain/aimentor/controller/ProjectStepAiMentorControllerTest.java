package com.example.seedbe.domain.aimentor.controller;

import com.example.seedbe.domain.aimentor.dto.AiMessageResponse;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.domain.aimentor.service.ProjectStepAiMentorService;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.exception.GlobalExceptionHandler;
import com.example.seedbe.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectStepAiMentorControllerTest {
    private final ProjectStepAiMentorService aiMentorService = mock(ProjectStepAiMentorService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectStepAiMentorController(aiMentorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticationPrincipalResolver(userDetails))
                .build();
    }

    @Test
    void createsAndGetsAiMessages() throws Exception {
        UUID turnId = UUID.randomUUID();
        AiMessageResponse userMessage = new AiMessageResponse(
                UUID.randomUUID(), turnId, AiMessageSender.USER, AiMessageType.CHAT,
                "question", null, null, null, null);
        AiMessageResponse assistantMessage = new AiMessageResponse(
                UUID.randomUUID(), turnId, AiMessageSender.ASSISTANT, AiMessageType.CHAT,
                "answer", 10, 5, 15, null);
        String path = "/api/projects/" + projectId + "/steps/constraint_analysis/ai-messages";
        when(aiMentorService.createMessage(
                userId, projectId, "constraint_analysis", AiMessageType.CHAT, "question"))
                .thenReturn(List.of(userMessage, assistantMessage));
        when(aiMentorService.getMessages(userId, projectId, "constraint_analysis"))
                .thenReturn(List.of(userMessage, assistantMessage));

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"CHAT\",\"content\":\"question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sender").value("USER"))
                .andExpect(jsonPath("$.data[1].sender").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[1].totalTokens").value(15));
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("question"));

        verify(aiMentorService).createMessage(
                userId, projectId, "constraint_analysis", AiMessageType.CHAT, "question");
        verify(aiMentorService).getMessages(userId, projectId, "constraint_analysis");
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        String path = "/api/projects/" + projectId + "/steps/constraint_analysis/ai-messages";

        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"CHAT\",\"content\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("G001"));
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver(CustomUserDetails userDetails) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };
    }
}
