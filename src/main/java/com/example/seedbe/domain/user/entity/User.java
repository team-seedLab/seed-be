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
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = {"provider", "provider_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 (안전하게 protected)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false)
    private String provider; // GOOGLE, KAKAO, LOCAL

    @Column(name = "provider_id")
    private String providerId; // OAuth 고유 식별자, LOCAL은 null

    @Column(nullable = false)
    private String email;

    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role; // 권한 (ROLE_USER)

    @Column(name = "profile_url", columnDefinition = "TEXT")
    private String profileUrl;

    @Builder
    public User(String provider, String providerId, String email, String password, String nickname, Role role, String profileUrl) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.profileUrl = profileUrl;
    }

    public User updateProfile(String nickname, String profileUrl) {
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        return this;
    }
}
