package com.example.seedbe.domain.project.controller;

import com.example.seedbe.domain.project.dto.ProjectListResponse;
import com.example.seedbe.domain.project.service.ProjectService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.common.response.PageResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<PageResponse<ProjectListResponse>> getProjects(
            @AuthenticationPrincipal CustomUserDetails user, // 보안 인증 객체 (본인 것만 조회)
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<ProjectListResponse> dtoPage = projectService.getProjects(user.getUser().getUserId(), pageable);

        PageResponse<ProjectListResponse> customPageResponse = PageResponse.of(dtoPage);

        return ApiResponse.success(customPageResponse);
    }
}
