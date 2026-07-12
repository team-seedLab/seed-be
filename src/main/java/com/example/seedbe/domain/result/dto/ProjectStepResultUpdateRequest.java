package com.example.seedbe.domain.result.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectStepResultUpdateRequest(
        @NotBlank(message = "결과물은 비어있을 수 없습니다.")
        @Size(max = 100_000, message = "결과물은 100000자를 초과할 수 없습니다.")
        String contentMarkdown
) {
}
