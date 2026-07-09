package com.example.seedbe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MentorLoginRequest(

        @Schema(description = "멘토 로그인 이메일", example = "mentor@seed.com")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "멘토 로그인 비밀번호", example = "mentor1234!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
