package com.example.seedbe.domain.result.controller;

import com.example.seedbe.domain.result.dto.ProjectStepResultResponse;
import com.example.seedbe.domain.result.service.ProjectStepResultService;
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

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectStepResultControllerTest {
    private final ProjectStepResultService resultService = mock(ProjectStepResultService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectStepResultController(resultService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(userDetails))
                .build();
    }

    @Test
    void savesAndGetsMarkdownResult() throws Exception {
        ProjectStepResultResponse response = new ProjectStepResultResponse(
                UUID.randomUUID(), "constraint_analysis", "제약사항 분석", "# 결과물", null, null);
        when(resultService.saveResult(userId, projectId, "constraint_analysis", "# 결과물"))
                .thenReturn(response);
        when(resultService.getResult(userId, projectId, "constraint_analysis")).thenReturn(response);
        String path = "/api/projects/" + projectId + "/steps/constraint_analysis/result";

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMarkdown\":\"# 결과물\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMarkdown").value("# 결과물"));
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepCode").value("constraint_analysis"));

        verify(resultService).saveResult(userId, projectId, "constraint_analysis", "# 결과물");
        verify(resultService).getResult(userId, projectId, "constraint_analysis");
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
