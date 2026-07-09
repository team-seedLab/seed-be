package com.example.seedbe.domain.auth.service;

import com.example.seedbe.domain.auth.dto.MentorLoginRequest;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.domain.user.repository.UserRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.example.seedbe.global.security.CustomUserDetails;
import com.example.seedbe.global.security.jwt.JwtProperties;
import com.example.seedbe.global.security.jwt.JwtProvider;
import com.example.seedbe.global.security.jwt.RefreshTokenService;
import com.example.seedbe.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public void mentorLogin(MentorLoginRequest request, HttpServletResponse response, boolean isSecure) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (!"LOCAL".equalsIgnoreCase(user.getProvider())) {
            throw new BusinessException(ErrorType.FORBIDDEN_ACCESS);
        }

        if (user.getRole() != Role.ROLE_MENTOR) {
            throw new BusinessException(ErrorType.FORBIDDEN_ACCESS);
        }

        String userId = user.getUserId().toString();

        String accessToken = jwtProvider.createAccessToken(userId, user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(userId);

        refreshTokenService.saveRefreshToken(userId, refreshToken);

        log.info("유저 ID: {} 토큰 발급 완료", userId);

        int accessCookieMaxAge = (int) (jwtProperties.accessTokenExpiration() / 1000);
        CookieUtil.addCookie(response, "accessToken", accessToken, accessCookieMaxAge, isSecure, "/");

        int refreshCookieMaxAge = (int) (jwtProperties.refreshTokenExpiration() / 1000);
        CookieUtil.addCookie(response, "refreshToken", refreshToken, refreshCookieMaxAge, isSecure, "/api/auth");
    }

    @Transactional
    public void reissueToken(String refreshToken, HttpServletResponse response, Boolean isSecure) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorType.INVALID_TOKEN);
        }

        String userId = jwtProvider.getUserIdFromToken(refreshToken);

        String savedRefreshTokenHash = refreshTokenService.getRefreshTokenHash(userId);
        if (savedRefreshTokenHash == null) {
            throw new BusinessException(ErrorType.INVALID_TOKEN);
        }

        if (!refreshTokenService.matchesRefreshTokenHash(savedRefreshTokenHash, refreshToken)) {
            log.warn("유저 ID: {} - Refresh Token 재사용 의심으로 세션 삭제", userId);
            refreshTokenService.deleteRefreshToken(userId);
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
