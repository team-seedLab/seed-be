package com.example.seedbe.domain.user.dto;

import com.example.seedbe.domain.user.entity.User;

import java.util.UUID;

public record UserDetailResponse(
        UUID userId,
        String nickname,
        String profileUrl
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getUserId(),
                user.getNickname(),
                user.getProfileUrl()
        );
    }
}
