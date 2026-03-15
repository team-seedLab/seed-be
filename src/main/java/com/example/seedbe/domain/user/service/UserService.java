package com.example.seedbe.domain.user.service;

import com.example.seedbe.domain.user.dto.UserDetailResponse;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDetailResponse getUserDetails(User user) {
        return UserDetailResponse.from(user);
    }
}
