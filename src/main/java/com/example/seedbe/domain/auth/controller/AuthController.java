package com.example.seedbe.domain.auth.controller;

import com.example.seedbe.domain.auth.service.AuthService;
import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.example.seedbe.global.security.CustomUserDetails;
import com.example.seedbe.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final Environment environment;

    @PostMapping("/reissue")
    public ApiResponse<String> reissue(
            // 프론트엔드가 API를 찌를 때 브라우저가 알아서 실어 보낸 쿠키에서 추출
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ){
        if (refreshToken == null) {
            throw new BusinessException(ErrorType.INVALID_TOKEN);
        }

        boolean isSecure = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        authService.reissueToken(refreshToken, response, isSecure);

        return ApiResponse.success("토큰이 성공적으로 재발급 되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(
            // 현재 로그인한 유저의 정보를 시큐리티 컨텍스트에서 뽑아옴
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ) {
        String userId = userDetails.getUser().getUserId().toString();

        authService.logout(userId);

        boolean isSecure = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        CookieUtil.deleteCookie(response, "accessToken", isSecure, "/");
        CookieUtil.deleteCookie(response, "refreshToken", isSecure, "/api/auth");

        return ApiResponse.success("성공적으로 로그아웃 되었습니다.");
    }
}
