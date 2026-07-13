package com.example.seedbe.domain.selfcheck.controller;

import com.example.seedbe.domain.selfcheck.dto.ProjectStepSelfCheckResponse;
import com.example.seedbe.domain.selfcheck.dto.ProjectStepSelfCheckUpdateRequest;
import com.example.seedbe.domain.selfcheck.service.ProjectStepSelfCheckService;
import com.example.seedbe.global.common.constants.SwaggerConstants;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "ProjectStep SelfCheck Controller", description = "프로젝트 단계 이해 확인 API")
@RestController
@RequestMapping("/api/projects/{projectId}/steps/{stepCode}/self-check")
@RequiredArgsConstructor
public class ProjectStepSelfCheckController {
    private final ProjectStepSelfCheckService selfCheckService;

    @Operation(summary = "단계 이해 확인 저장 및 수정 (로그인 필요)")
    @PutMapping
    public ApiResponse<ProjectStepSelfCheckResponse> saveSelfCheck(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION) @PathVariable String stepCode,
            @Valid @RequestBody ProjectStepSelfCheckUpdateRequest request) {
        return ApiResponse.success(selfCheckService.saveSelfCheck(
                user.getUser().getUserId(), projectId, stepCode, request.checkItems()));
    }

    @Operation(summary = "단계 이해 확인 조회 (로그인 필요)")
    @GetMapping
    public ApiResponse<ProjectStepSelfCheckResponse> getSelfCheck(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION) @PathVariable String stepCode) {
        return ApiResponse.success(selfCheckService.getSelfCheck(
                user.getUser().getUserId(), projectId, stepCode));
    }
}
