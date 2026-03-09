package com.example.seedbe.global.security;

import com.example.seedbe.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {

    private final User user;
    private Map<String, Object> attributes; // 소셜에서 넘어온 원본 데이터

    // 일반 로그인용
    public CustomUserDetails(User user) {
        this.user = user;
    }

    // 소셜 로그인용
    public CustomUserDetails(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    // 일반 로그인용
    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return user.getUserId().toString(); }

    public User getUser() { return user; }

    // 소셜 로그인용
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String getName() { return user.getUserId().toString(); }

    // 아래 4개는 계정 만료, 잠김 여부인데 일단 다 true로 세팅
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
