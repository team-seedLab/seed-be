package com.example.seedbe.domain.user.controller;

import com.example.seedbe.domain.user.dto.UserDetailResponse;
import com.example.seedbe.domain.user.service.UserService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Controller", description = "유저 관련 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "유저 정보 가져오기",
            description = "유저 정보를 가져옵니다"
    )
    @GetMapping("/me")
    public ApiResponse<UserDetailResponse> getUserDetails(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UserDetailResponse response = userService.getUserDetails(user.getUser());
        return ApiResponse.success(response);
    }
}
