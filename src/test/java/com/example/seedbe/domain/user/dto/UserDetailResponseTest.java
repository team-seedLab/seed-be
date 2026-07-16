package com.example.seedbe.domain.user.dto;

import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailResponseTest {

    @Test
    void includesUserRole() {
        User user = User.builder()
                .provider("LOCAL")
                .email("user@example.com")
                .nickname("seed")
                .role(Role.ROLE_USER)
                .profileUrl("https://example.com/profile.png")
                .build();

        UserDetailResponse response = UserDetailResponse.from(user);

        assertThat(response.role()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void includesMentorRole() {
        User mentor = User.builder()
                .provider("LOCAL")
                .email("mentor@example.com")
                .nickname("mentor")
                .role(Role.ROLE_MENTOR)
                .profileUrl("https://example.com/mentor.png")
                .build();

        UserDetailResponse response = UserDetailResponse.from(mentor);

        assertThat(response.role()).isEqualTo(Role.ROLE_MENTOR);
    }
}
