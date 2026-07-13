package com.example.seedbe.domain.aimentor.dto;

import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiMessageCreateRequest(
        @NotNull(message = "메시지 유형은 필수입니다.")
        AiMessageType messageType,

        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 2000, message = "질문은 2,000자를 초과할 수 없습니다.")
        String content
) {
}
