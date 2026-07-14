package com.example.seedbe.domain.mentor.controller;

import com.example.seedbe.domain.mentor.dto.MentorDashboardSummary;
import com.example.seedbe.domain.mentor.dto.MentorProjectDetailResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentListResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentProjectListResponse;
import com.example.seedbe.domain.mentor.dto.ProjectReviewResponse;
import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import com.example.seedbe.domain.mentor.service.MentorService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MentorControllerTest {
    private final MentorService mentorService = mock(MentorService.class);
    private final UUID mentorId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User mentor = mock(User.class);
        when(mentor.getUserId()).thenReturn(mentorId);
        CustomUserDetails userDetails = new CustomUserDetails(mentor);
        mockMvc = MockMvcBuilders.standaloneSetup(new MentorController(mentorService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(userDetails))
                .build();
    }

    @Test
    void routesMentorReadApis() throws Exception {
        when(mentorService.getStudents(mentorId)).thenReturn(
                new MentorStudentListResponse(new MentorDashboardSummary(0, 0, 0), List.of()));
        when(mentorService.getStudentProjects(mentorId, studentId)).thenReturn(
                new MentorStudentProjectListResponse(studentId, "student", "student@seed.test", null, List.of()));
        when(mentorService.getProjectDetail(mentorId, projectId)).thenReturn(
                new MentorProjectDetailResponse(projectId, studentId, "student", "title", null, null,
                        null, null, null, null, null, null, null, null, List.of()));

        mockMvc.perform(get("/api/mentor/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.totalStudentCount").value(0));
        mockMvc.perform(get("/api/mentor/students/{studentId}/projects", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()));
        mockMvc.perform(get("/api/mentor/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.data.aiMessages").doesNotExist());

        verify(mentorService).getStudents(mentorId);
        verify(mentorService).getStudentProjects(mentorId, studentId);
        verify(mentorService).getProjectDetail(mentorId, projectId);
    }

    @Test
    void routesProjectReviewUpdate() throws Exception {
        ProjectReviewResponse response = new ProjectReviewResponse(
                UUID.randomUUID(), projectId, ProjectReviewStatus.REVIEWED,
                LocalDateTime.now(), null, null);
        when(mentorService.completeProjectReview(mentorId, projectId)).thenReturn(response);

        mockMvc.perform(patch("/api/mentor/projects/{projectId}/review", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWED"));

        verify(mentorService).completeProjectReview(mentorId, projectId);
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
