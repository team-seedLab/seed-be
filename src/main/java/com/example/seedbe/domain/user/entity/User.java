package com.example.seedbe.domain.user.entity;

import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 (안전하게 protected)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false)
    private String provider; // 제공자: google, kakao

    @Column(name = "provider_id", nullable = false)
    private String providerId; // 소셜 고유 식별자

    private String email;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role; // 권한 (ROLE_USER)

    @Column(name = "profile_url", columnDefinition = "TEXT")
    private String profileUrl;

    @Builder
    public User(String provider, String providerId, String email, String nickname, Role role, String profileUrl) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.profileUrl = profileUrl;
    }
}
