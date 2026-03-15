package com.example.seedbe.domain.user.service;

import com.example.seedbe.domain.user.dto.UserDetailResponse;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.repository.UserRepository;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        return UserDetailResponse.from(user);
    }
}
