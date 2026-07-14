package com.example.seedbe.domain.selfcheck.controller;

import com.example.seedbe.domain.selfcheck.dto.ProjectStepSelfCheckResponse;
import com.example.seedbe.domain.selfcheck.dto.SelfCheckItemRequest;
import com.example.seedbe.domain.selfcheck.model.SelfCheckItem;
import com.example.seedbe.domain.selfcheck.service.ProjectStepSelfCheckService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectStepSelfCheckControllerTest {
    private final ProjectStepSelfCheckService selfCheckService = mock(ProjectStepSelfCheckService.class);
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectStepSelfCheckController(selfCheckService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(userDetails))
                .build();
    }

    @Test
    void savesAndGetsSelfCheckItems() throws Exception {
        String answer = "핵심 제약사항을 구체적으로 이해했습니다.";
        List<SelfCheckItemRequest> requests = List.of(
                new SelfCheckItemRequest("core_understanding", answer),
                new SelfCheckItemRequest("result_application", answer),
                new SelfCheckItemRequest("uncertainty_review", answer));
        ProjectStepSelfCheckResponse response = new ProjectStepSelfCheckResponse(
                UUID.randomUUID(), UUID.randomUUID(), "constraint_analysis", "제약사항 분석",
                List.of(new SelfCheckItem("core_understanding",
                        "이번 단계에서 이해한 핵심 내용을 자신의 말로 설명해 주세요.", answer)),
                LocalDateTime.now(), null, null);
        when(selfCheckService.saveSelfCheck(userId, projectId, "constraint_analysis", requests))
                .thenReturn(response);
        when(selfCheckService.getSelfCheck(userId, projectId, "constraint_analysis")).thenReturn(response);
        String path = "/api/projects/" + projectId + "/steps/constraint_analysis/self-check";

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checkItems":[
                                  {"key":"core_understanding","answer":"핵심 제약사항을 구체적으로 이해했습니다."},
                                  {"key":"result_application","answer":"핵심 제약사항을 구체적으로 이해했습니다."},
                                  {"key":"uncertainty_review","answer":"핵심 제약사항을 구체적으로 이해했습니다."}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkItems[0].key").value("core_understanding"))
                .andExpect(jsonPath("$.data.checkItems[0].answer").value(answer));
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepCode").value("constraint_analysis"));

        verify(selfCheckService).saveSelfCheck(userId, projectId, "constraint_analysis", requests);
        verify(selfCheckService).getSelfCheck(userId, projectId, "constraint_analysis");
    }

    @Test
    void rejectsBlankAnswer() throws Exception {
        String path = "/api/projects/" + projectId + "/steps/constraint_analysis/self-check";

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checkItems":[
                                  {"key":"core_understanding","answer":"   "},
                                  {"key":"result_application","answer":"충분한 길이의 두 번째 답변입니다."},
                                  {"key":"uncertainty_review","answer":"충분한 길이의 세 번째 답변입니다."}
                                ]}
                                """))
                .andExpect(status().isBadRequest());
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
