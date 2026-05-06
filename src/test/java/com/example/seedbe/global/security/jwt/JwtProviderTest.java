package com.example.seedbe.global.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi1tdXN0LWJlLWxvbmc=",
            3_600_000L,
            1_209_600_000L
    ));

    @Test
    @DisplayName("access token은 access token 검증만 통과한다.")
    void accessTokenPassesOnlyAccessTokenValidation() {
        String accessToken = jwtProvider.createAccessToken("user-id", "ROLE_USER");

        assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtProvider.validateRefreshToken(accessToken)).isFalse();
    }

    @Test
    @DisplayName("refresh token은 refresh token 검증만 통과한다.")
    void refreshTokenPassesOnlyRefreshTokenValidation() {
        String refreshToken = jwtProvider.createRefreshToken("user-id");

        assertThat(jwtProvider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(jwtProvider.validateAccessToken(refreshToken)).isFalse();
    }
}
