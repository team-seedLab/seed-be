package com.example.seedbe.domain.auth.service;

import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.repository.UserRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.example.seedbe.global.security.jwt.JwtProperties;
import com.example.seedbe.global.security.jwt.JwtProvider;
import com.example.seedbe.global.security.jwt.RefreshTokenService;
import com.example.seedbe.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public void reissueToken(String refreshToken, HttpServletResponse response, Boolean isSecure) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorType.INVALID_TOKEN);
        }

        String userId = jwtProvider.getUserIdFromToken(refreshToken);

        String savedRefreshToken = refreshTokenService.getRefreshToken(userId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorType.INVALID_TOKEN);
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        refreshTokenService.saveRefreshToken(userId, newRefreshToken);
        log.info("유저 ID: {} 토큰 재발급(RTR) 완료", userId);

        int accessCookieMaxAge = (int) (jwtProperties.accessTokenExpiration() / 1000);
        CookieUtil.addCookie(response, "accessToken", newAccessToken, accessCookieMaxAge, isSecure, "/");

        int refreshCookieMaxAge = (int) (jwtProperties.refreshTokenExpiration() / 1000);
        CookieUtil.addCookie(response, "refreshToken", newRefreshToken, refreshCookieMaxAge, isSecure, "/api/auth");

    }

    @Transactional
    public void logout(String userId) {
        // Redis에 저장된 Refresh Token을 날려버림!
        refreshTokenService.deleteRefreshToken(userId);
        log.info("유저 ID: {} 로그아웃 완료 (Redis RT 삭제)", userId);
    }
}
