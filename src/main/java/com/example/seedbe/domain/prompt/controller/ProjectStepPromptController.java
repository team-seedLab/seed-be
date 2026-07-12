package com.example.seedbe.domain.prompt.controller;

import com.example.seedbe.domain.prompt.dto.ProjectStepPromptResponse;
import com.example.seedbe.domain.prompt.dto.ProjectStepPromptUpdateRequest;
import com.example.seedbe.domain.prompt.service.ProjectStepPromptService;
import com.example.seedbe.global.common.constants.SwaggerConstants;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "ProjectStep Prompt Controller", description = "프로젝트 단계 프롬프트 API")
@RestController
@RequestMapping("/api/projects/{projectId}/steps/{stepCode}/prompt")
@RequiredArgsConstructor
public class ProjectStepPromptController {
    private final ProjectStepPromptService promptService;

    @Operation(summary = "단계 최초 프롬프트 생성 또는 재조회 (로그인 필요)")
    @PostMapping
    public ApiResponse<ProjectStepPromptResponse> createPrompt(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION) @PathVariable String stepCode) {
        return ApiResponse.success(promptService.createPrompt(
                user.getUser().getUserId(), projectId, stepCode));
    }

    @Operation(summary = "단계 프롬프트 상세 조회 (로그인 필요)")
    @GetMapping
    public ApiResponse<ProjectStepPromptResponse> getPrompt(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @PathVariable String stepCode) {
        return ApiResponse.success(promptService.getPrompt(
                user.getUser().getUserId(), projectId, stepCode));
    }

    @Operation(summary = "단계 프롬프트 저장 및 수정 (로그인 필요)")
    @PutMapping
    public ApiResponse<ProjectStepPromptResponse> updatePrompt(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @PathVariable String stepCode,
            @Valid @RequestBody ProjectStepPromptUpdateRequest request) {
        return ApiResponse.success(promptService.updatePrompt(
                user.getUser().getUserId(), projectId, stepCode, request.editedPrompt()));
    }
}
