package com.example.seedbe.domain.project.controller;

import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.service.ProjectStepService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "ProjectStep Controller", description = "프로젝트 단계 관련 API")
@RestController
@RequestMapping("/api/projects/{projectId}/steps")
@RequiredArgsConstructor
public class ProjectStepController {
    private final ProjectStepService projectStepService;

    @Operation(
            summary = "단계 시작 및 프롬프트 발급",
            description = "해당 단계의 변수가 치환된 프롬프트를 발급하고 로그를 저장합니다.")
    @PostMapping("/{stepCode}/start")
    public ApiResponse<ProjectPromptStepResponse> startStep(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @PathVariable String stepCode) {

        ProjectPromptStepResponse response = projectStepService.createAndSavePrompt(user.getUser().getUserId(), projectId, stepCode);

        return ApiResponse.success(response);
    }
}
