package com.example.seedbe.global.security.oauth2;

import com.example.seedbe.global.security.CustomUserDetails;
import com.example.seedbe.global.security.jwt.JwtProperties;
import com.example.seedbe.global.security.jwt.JwtProvider;
import com.example.seedbe.global.security.jwt.RefreshTokenService;
import com.example.seedbe.global.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final OAuth2Properties oAuth2Properties;
    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUser().getUserId().toString();
        String role = userDetails.getUser().getRole().name();

        String accessToken = jwtProvider.createAccessToken(userId, role);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        refreshTokenService.saveRefreshToken(userId, refreshToken);
        log.info("로그인 성공! 유저 ID: {}, AccessToken/RefreshToken 발급 완료", userId);

        //  Environment를 써서 현재 활성화된 프로필 중 prod가 있는지 검사
        boolean isSecure = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        int accessCookieMaxAge = (int) (jwtProperties.accessTokenExpiration() / 1000);
        CookieUtil.addCookie(response, "accessToken", accessToken, accessCookieMaxAge, isSecure, "/");

        int refreshCookieMaxAge = (int) (jwtProperties.refreshTokenExpiration() / 1000);
        CookieUtil.addCookie(response, "refreshToken", refreshToken, refreshCookieMaxAge, isSecure, "/api/auth");

        // 동적 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, oAuth2Properties.redirectUri());
    }
}
