package com.example.seedbe.domain.aimentor.controller;

import com.example.seedbe.domain.aimentor.dto.AiMessageCreateRequest;
import com.example.seedbe.domain.aimentor.dto.AiMessageResponse;
import com.example.seedbe.domain.aimentor.service.ProjectStepAiMentorService;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "ProjectStep AI Mentor Controller", description = "프로젝트 단계 AI 멘토 대화 API")
@RestController
@RequestMapping("/api/projects/{projectId}/steps/{stepCode}/ai-messages")
@RequiredArgsConstructor
public class ProjectStepAiMentorController {
    private final ProjectStepAiMentorService aiMentorService;

    @Operation(summary = "단계 AI 멘토 대화 내역 조회 (로그인 필요)")
    @GetMapping
    public ApiResponse<List<AiMessageResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION) @PathVariable String stepCode) {
        return ApiResponse.success(aiMentorService.getMessages(
                user.getUser().getUserId(), projectId, stepCode));
    }

    @Operation(summary = "단계 AI 멘토 질문 전송 (로그인 필요)")
    @PostMapping
    public ApiResponse<List<AiMessageResponse>> createMessage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId,
            @Parameter(description = SwaggerConstants.STEP_CODE_DESCRIPTION) @PathVariable String stepCode,
            @Valid @RequestBody AiMessageCreateRequest request) {
        return ApiResponse.success(aiMentorService.createMessage(
                user.getUser().getUserId(), projectId, stepCode,
                request.messageType(), request.content()));
    }
}
