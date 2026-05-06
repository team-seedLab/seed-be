package com.example.seedbe.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.HexFormat;

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
                hashToken(refreshToken),
                expirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    public String getRefreshTokenHash(String userId) {
        return refreshTokenRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + userId);
    }

    public boolean matchesRefreshTokenHash(String savedRefreshTokenHash, String refreshToken) {
        if (savedRefreshTokenHash == null) {
            return false;
        }

        byte[] savedRefreshTokenHashBytes = HexFormat.of().parseHex(savedRefreshTokenHash);
        byte[] requestRefreshTokenHashBytes = HexFormat.of().parseHex(hashToken(refreshToken));

        return MessageDigest.isEqual(savedRefreshTokenHashBytes, requestRefreshTokenHashBytes);
    }

    public void deleteRefreshToken(String userId) {
        refreshTokenRedisTemplate.delete(REDIS_KEY_PREFIX + userId);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
