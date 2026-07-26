package com.example.seedbe.domain.analysis.controller;

import com.example.seedbe.domain.analysis.dto.ProjectDependencyAnalysisResponse;
import com.example.seedbe.domain.analysis.service.ProjectDependencyAnalysisService;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectDependencyAnalysisControllerTest {
    private final ProjectDependencyAnalysisService analysisService =
            mock(ProjectDependencyAnalysisService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ProjectDependencyAnalysisController(analysisService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(userDetails))
                .build();
    }

    @Test
    void routesProjectDependencyAnalysisApi() throws Exception {
        ProjectDependencyAnalysisResponse response = new ProjectDependencyAnalysisResponse(
                UUID.randomUUID(), 70, 40, 30, null, null);
        when(analysisService.getAnalysis(userId, projectId)).thenReturn(response);

        mockMvc.perform(get("/api/projects/{projectId}/dependency-analysis", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initiativeScore").value(70))
                .andExpect(jsonPath("$.data.userEditRatio").value(40))
                .andExpect(jsonPath("$.data.aiDependencyRatio").value(30));

        verify(analysisService).getAnalysis(userId, projectId);
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
