package com.example.seedbe.domain.prompt.controller;

import com.example.seedbe.domain.prompt.dto.ProjectStepPromptResponse;
import com.example.seedbe.domain.prompt.service.ProjectStepPromptService;
import com.example.seedbe.domain.user.entity.User;
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

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectStepPromptControllerTest {
    private final ProjectStepPromptService promptService = mock(ProjectStepPromptService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectStepPromptController(promptService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(userDetails))
                .build();
    }

    @Test
    void createsGetsAndUpdatesStepPromptThroughSeparatedEndpoints() throws Exception {
        ProjectStepPromptResponse created = response(null, "provided", 0, 0);
        ProjectStepPromptResponse edited = response("provided edited", "provided edited", 7, 0);
        when(promptService.createPrompt(userId, projectId, "constraint_analysis")).thenReturn(created);
        when(promptService.getPrompt(userId, projectId, "constraint_analysis")).thenReturn(created);
        when(promptService.updatePrompt(userId, projectId, "constraint_analysis", "provided edited"))
                .thenReturn(edited);

        String path = "/api/projects/" + projectId + "/steps/constraint_analysis/prompt";
        mockMvc.perform(post(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providedPromptSnapshot").value("provided"));
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalPrompt").value("provided"));
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"editedPrompt\":\"provided edited\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.editedPrompt").value("provided edited"))
                .andExpect(jsonPath("$.data.addedCount").value(7));

        verify(promptService).createPrompt(userId, projectId, "constraint_analysis");
        verify(promptService).getPrompt(userId, projectId, "constraint_analysis");
        verify(promptService).updatePrompt(userId, projectId, "constraint_analysis", "provided edited");
    }

    private ProjectStepPromptResponse response(String editedPrompt, String finalPrompt,
                                               int addedCount, int removedCount) {
        return new ProjectStepPromptResponse(UUID.randomUUID(), "constraint_analysis", "제약사항 분석",
                "provided", editedPrompt, finalPrompt, "format", addedCount, removedCount,
                Map.of("version", "PREFIX_SUFFIX_V1", "segments", java.util.List.of()), null, null);
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
