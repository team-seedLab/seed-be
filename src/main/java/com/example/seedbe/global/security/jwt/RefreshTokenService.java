package com.example.seedbe.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final StringRedisTemplate refreshTokenRedisTemplate;
    private final JwtProperties jwtProperties;

    private static final String REDIS_KEY_PREFIX = "RT:";

    public void saveRefreshToken(String userId ,final String refreshToken) {
        long expirationMs = jwtProperties.refreshTokenExpiration();

        refreshTokenRedisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + userId,
                refreshToken,
                expirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    public String getRefreshToken(String userId) {
        return refreshTokenRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + userId);
    }

    public void deleteRefreshToken(String userId) {
        refreshTokenRedisTemplate.delete(REDIS_KEY_PREFIX + userId);
    }
}
