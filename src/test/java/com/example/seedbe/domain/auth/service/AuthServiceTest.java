package com.example.seedbe.domain.auth.service;

import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.domain.user.repository.UserRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.security.jwt.JwtProperties;
import com.example.seedbe.global.security.jwt.JwtProvider;
import com.example.seedbe.global.security.jwt.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("정상 refresh token이면 새 access/refresh token을 발급하고 refresh token hash를 저장한다.")
    void reissueTokenSucceedsWithValidRefreshToken() {
        JwtProperties jwtProperties = new JwtProperties("secret", 3_600_000L, 1_209_600_000L);
        AuthService authService = new AuthService(jwtProvider, refreshTokenService, userRepository, jwtProperties);

        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        String refreshToken = "valid-refresh-token";
        String savedRefreshTokenHash = "saved-refresh-token-hash";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        User user = User.builder()
                .provider("google")
                .providerId("provider-id")
                .email("seed@example.com")
                .nickname("seed")
                .role(Role.ROLE_USER)
                .profileUrl("https://example.com/profile.png")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(refreshToken)).thenReturn(userIdString);
        when(refreshTokenService.getRefreshTokenHash(userIdString)).thenReturn(savedRefreshTokenHash);
        when(refreshTokenService.matchesRefreshTokenHash(savedRefreshTokenHash, refreshToken)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userIdString, Role.ROLE_USER.name())).thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(userIdString)).thenReturn(newRefreshToken);

        authService.reissueToken(refreshToken, response, true);

        verify(refreshTokenService).saveRefreshToken(userIdString, newRefreshToken);
        verify(refreshTokenService, never()).deleteRefreshToken(userIdString);
        assertThat(response.getCookie("accessToken").getValue()).isEqualTo(newAccessToken);
        assertThat(response.getCookie("refreshToken").getValue()).isEqualTo(newRefreshToken);
        assertThat(response.getCookie("refreshToken").getPath()).isEqualTo("/api/auth");
    }

    @Test
    @DisplayName("저장 hash와 요청 refresh token hash가 불일치하면 reuse 의심으로 Redis refresh token을 삭제한다.")
    void reissueTokenDeletesRefreshTokenWhenHashDoesNotMatch() {
        JwtProperties jwtProperties = new JwtProperties("secret", 3_600_000L, 1_209_600_000L);
        AuthService authService = new AuthService(jwtProvider, refreshTokenService, userRepository, jwtProperties);

        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        String rotatedRefreshToken = "rotated-refresh-token";
        String savedRefreshTokenHash = "saved-refresh-token-hash";

        when(jwtProvider.validateToken(rotatedRefreshToken)).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(rotatedRefreshToken)).thenReturn(userIdString);
        when(refreshTokenService.getRefreshTokenHash(userIdString)).thenReturn(savedRefreshTokenHash);
        when(refreshTokenService.matchesRefreshTokenHash(savedRefreshTokenHash, rotatedRefreshToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.reissueToken(rotatedRefreshToken, new MockHttpServletResponse(), true))
                .isInstanceOf(BusinessException.class);

        verify(refreshTokenService).deleteRefreshToken(userIdString);
        verify(userRepository, never()).findById(userId);
    }
}
