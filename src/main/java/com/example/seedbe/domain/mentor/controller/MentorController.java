package com.example.seedbe.domain.mentor.controller;

import com.example.seedbe.domain.mentor.dto.MentorProjectDetailResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentListResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentProjectListResponse;
import com.example.seedbe.domain.mentor.dto.ProjectReviewResponse;
import com.example.seedbe.domain.mentor.service.MentorService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Mentor Controller", description = "멘토 학생 및 프로젝트 검토 API")
@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {
    private final MentorService mentorService;

    @Operation(summary = "배정 학생 목록 및 대시보드 요약 조회 (멘토 로그인 필요)")
    @GetMapping("/students")
    public ApiResponse<MentorStudentListResponse> getStudents(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success(mentorService.getStudents(user.getUser().getUserId()));
    }

    @Operation(summary = "배정 학생의 프로젝트 목록 조회 (멘토 로그인 필요)")
    @GetMapping("/students/{studentId}/projects")
    public ApiResponse<MentorStudentProjectListResponse> getStudentProjects(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID studentId) {
        return ApiResponse.success(mentorService.getStudentProjects(
                user.getUser().getUserId(), studentId));
    }

    @Operation(summary = "배정 학생의 프로젝트 상세 조회 (멘토 로그인 필요)")
    @GetMapping("/projects/{projectId}")
    public ApiResponse<MentorProjectDetailResponse> getProjectDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        return ApiResponse.success(mentorService.getProjectDetail(
                user.getUser().getUserId(), projectId));
    }

    @Operation(summary = "프로젝트 검토 완료 처리 (멘토 로그인 필요)")
    @PatchMapping("/projects/{projectId}/review")
    public ApiResponse<ProjectReviewResponse> completeProjectReview(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {
        return ApiResponse.success(mentorService.completeProjectReview(
                user.getUser().getUserId(), projectId));
    }
}
