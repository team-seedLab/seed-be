package com.example.seedbe.domain.analysis.controller;

import com.example.seedbe.domain.analysis.dto.ProjectDependencyAnalysisResponse;
import com.example.seedbe.domain.analysis.service.ProjectDependencyAnalysisService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Project Dependency Analysis Controller", description = "프로젝트 AI 활용 과정 분석 API")
@RestController
@RequestMapping("/api/projects/{projectId}/dependency-analysis")
@RequiredArgsConstructor
public class ProjectDependencyAnalysisController {
    private final ProjectDependencyAnalysisService analysisService;

    @Operation(
            summary = "프로젝트 AI 활용 과정 분석 조회 (로그인 필요)",
            description = "완료된 본인 프로젝트의 주도성, 직접 수정, AI 의존도 지표를 조회합니다."
    )
    @GetMapping
    public ApiResponse<ProjectDependencyAnalysisResponse> getAnalysis(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success(
                analysisService.getAnalysis(user.getUser().getUserId(), projectId)
        );
    }
}
