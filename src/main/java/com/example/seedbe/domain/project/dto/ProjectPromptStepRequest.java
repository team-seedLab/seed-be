package com.example.seedbe.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ProjectPromptStepRequest(
        @Schema(description = "유저가 외부 AI(ChatGPT 등)로부터 받아와 수정한 최종 결과물 텍스트",
                example = "1. 요구사항 분석 결과입니다. \n- 대상: 대학생\n- 목적: ...")
        @NotBlank(message = "결과물 텍스트는 비어있을 수 없습니다.")
        String resultText
) {
}
