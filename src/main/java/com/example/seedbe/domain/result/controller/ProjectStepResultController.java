package com.example.seedbe.domain.result.controller;

import com.example.seedbe.domain.result.dto.ProjectStepResultResponse;
import com.example.seedbe.domain.result.dto.ProjectStepResultUpdateRequest;
import com.example.seedbe.domain.result.service.ProjectStepResultService;
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

@Tag(name = "ProjectStep Result Controller", description = "프로젝트 단계 결과물 API")
@RestController
@RequestMapping("/api/projects/{projectId}/steps/{stepCode}/result")
@RequiredArgsConstructor
public class ProjectStepResultController {
    private final ProjectStepResultService resultService;

    @Operation(summary = "단계 결과물 저장 및 수정 (로그인 필요)")
    @PutMapping
    public ApiResponse<ProjectStepResultResponse> saveResult(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION) @PathVariable String stepCode,
            @Valid @RequestBody ProjectStepResultUpdateRequest request) {
        return ApiResponse.success(resultService.saveResult(
                user.getUser().getUserId(), projectId, stepCode, request.contentMarkdown()));
    }

    @Operation(summary = "단계 결과물 조회 (로그인 필요)")
    @GetMapping
    public ApiResponse<ProjectStepResultResponse> getResult(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @PathVariable String stepCode) {
        return ApiResponse.success(resultService.getResult(
                user.getUser().getUserId(), projectId, stepCode));
    }
}
