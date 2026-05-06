package com.example.seedbe.global.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate refreshTokenRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("refresh token 저장 시 Redis에는 원문 대신 SHA-256 hash가 저장된다.")
    void saveRefreshTokenStoresHash() {
        JwtProperties jwtProperties = new JwtProperties("secret", 3_600_000L, 1_209_600_000L);
        RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRedisTemplate, jwtProperties);

        when(refreshTokenRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String userId = "user-id";
        String refreshToken = "raw-refresh-token";

        refreshTokenService.saveRefreshToken(userId, refreshToken);

        ArgumentCaptor<String> savedValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq("RT:" + userId),
                savedValueCaptor.capture(),
                eq(jwtProperties.refreshTokenExpiration()),
                eq(TimeUnit.MILLISECONDS)
        );

        String savedValue = savedValueCaptor.getValue();
        assertThat(savedValue).isNotEqualTo(refreshToken);
        assertThat(savedValue).hasSize(64);
        assertThat(refreshTokenService.matchesRefreshTokenHash(savedValue, refreshToken)).isTrue();
    }

    @Test
    @DisplayName("저장 hash와 요청 refresh token hash가 다르면 false를 반환한다.")
    void matchesRefreshTokenHashReturnsFalseWhenTokenDoesNotMatch() {
        JwtProperties jwtProperties = new JwtProperties("secret", 3_600_000L, 1_209_600_000L);
        RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRedisTemplate, jwtProperties);

        String savedHash = "0".repeat(64);

        assertThat(refreshTokenService.matchesRefreshTokenHash(savedHash, "different-refresh-token")).isFalse();
    }
}
