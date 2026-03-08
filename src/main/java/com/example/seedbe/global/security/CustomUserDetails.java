package com.example.seedbe.global.security;

import com.example.seedbe.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        // 소셜 로그인이라 비밀번호가 없으므로 null 또는 빈 문자열 반환
        return null;
    }

    @Override
    public String getUsername() {
        // 시큐리티에서 식별자로 쓸 값
        return user.getUserId().toString();
    }

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
