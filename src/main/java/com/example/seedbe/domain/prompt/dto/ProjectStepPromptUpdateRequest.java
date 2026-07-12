package com.example.seedbe.domain.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectStepPromptUpdateRequest(
        @NotBlank(message = "수정 프롬프트는 비어있을 수 없습니다.")
        @Size(max = 20_000, message = "수정 프롬프트는 20000자를 초과할 수 없습니다.")
        String editedPrompt
) {
}
