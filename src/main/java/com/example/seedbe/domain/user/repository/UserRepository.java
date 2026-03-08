package com.example.seedbe.domain.user.repository;

import com.example.seedbe.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 소셜 로그인 시, 이미 가입된 유저인지 확인하기 위한 쿼리 메서드
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}