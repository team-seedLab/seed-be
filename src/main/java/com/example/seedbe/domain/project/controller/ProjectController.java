package com.example.seedbe.domain.project.controller;

import com.example.seedbe.domain.project.dto.ProjectCreateRequest;
import com.example.seedbe.domain.project.dto.ProjectDetailResponse;
import com.example.seedbe.domain.project.dto.ProjectSummaryResponse;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.service.ProjectService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.common.response.PageResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Project Controller", description = "프로젝트 관련 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @Operation(
            summary = "프로젝트 목록 조회 (페이징) (로그인 필요)",
            description = "로그인한 사용자의 프로젝트 목록을 최신순으로 페이징하여 조회합니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<ProjectSummaryResponse>> getProjects(
            @AuthenticationPrincipal CustomUserDetails user, // 보안 인증 객체 (본인 것만 조회)
            @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {

        Page<ProjectSummaryResponse> response = projectService.getProjects(user.getUser().getUserId(), status, pageable);

        PageResponse<ProjectSummaryResponse> customPageResponse = PageResponse.of(response);

        return ApiResponse.success(customPageResponse);
    }

    @Operation(
            summary = "단일 프로젝트 상세 조회 (로그인 필요)",
            description = "프로젝트 ID로 특정 프로젝트의 기본 정보와 지금까지 진행된 프롬프트 단계(steps) 이력을 한 번에 조회합니다."
    )
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> getProjectDetails(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId
    ) {
        ProjectDetailResponse response = projectService.getProjectDetails(user.getUser().getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프로젝트 생성 (로그인 필요)",
            description = "PDF 파일과 유저 입력값을 받아 AI로 파싱한 후, 템플릿 변수로 DB에 즉시 저장합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProjectSummaryResponse> createProject(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ModelAttribute ProjectCreateRequest projectCreateRequest
            ){
        ProjectSummaryResponse response = projectService.createProject(user.getUser(), projectCreateRequest);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프로젝트 삭제 (로그인 필요)",
            description = "프로젝트 ID로 특정 프로젝트를 삭제합니다."
    )
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId
    ) {
        projectService.deleteProject(user.getUser().getUserId(), projectId);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "프로젝트 완료 처리 (로그인 필요)",
            description = "진행 중인 프로젝트를 완료(COMPLETED) 상태로 변경합니다.")
    @PatchMapping("/{projectId}/complete")
    public ApiResponse<Void> completeProject(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID projectId) {

        projectService.completeProject(user.getUser().getUserId(), projectId);

        return ApiResponse.success(null);
    }
}
