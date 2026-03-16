package com.example.seedbe.domain.project.controller;

import com.example.seedbe.domain.project.dto.ProjectPromptStepRequest;
import com.example.seedbe.domain.project.dto.ProjectPromptStepResponse;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.service.ProjectStepService;
import com.example.seedbe.global.common.constants.SwaggerConstants;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "ProjectStep Controller", description = "프로젝트 단계 관련 API")
@RestController
@RequestMapping("/api/projects/{projectId}/steps")
@RequiredArgsConstructor
public class ProjectStepController {
    private final ProjectStepService projectStepService;

    @Operation(
            summary = "각 단계 프롬프트 발급",
            description = "해당 단계의 변수가 치환된 프롬프트를 발급하고 로그를 저장합니다.")
    @PostMapping("/{stepCode}")
    public ApiResponse<ProjectPromptStepResponse> startStep(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION)
            @PathVariable String stepCode) {

        ProjectPromptStepResponse response = projectStepService.createAndSavePrompt(user.getUser().getUserId(), projectId, stepCode);

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "단계 결과물 저장 및 수정",
            description = "발급받은 프롬프트를 바탕으로 생성한 결과물을 저장하거나 수정(덮어쓰기)합니다."
    )
    @PatchMapping("/{stepCode}/result")
    public ApiResponse<Void> saveStepResult(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION)
            @PathVariable String stepCode,
            @Valid @RequestBody ProjectPromptStepRequest request) {

        projectStepService.saveStepResult(user.getUser().getUserId(), projectId, stepCode, request.resultText()
        );

        return ApiResponse.success(null);
    }
}
