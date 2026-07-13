package com.example.seedbe.domain.selfcheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SelfCheckItemRequest(
        @NotBlank(message = "문항 key는 비어있을 수 없습니다.")
        @Size(max = 50, message = "문항 key는 50자를 초과할 수 없습니다.")
        String key,

        @NotBlank(message = "이해 확인 답변은 비어있을 수 없습니다.")
        @Size(max = 5_000, message = "이해 확인 답변은 5000자를 초과할 수 없습니다.")
        String answer
) {
}
