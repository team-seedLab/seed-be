package com.example.seedbe.domain.selfcheck.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectStepSelfCheckUpdateRequest(
        @NotEmpty(message = "이해 확인 문항은 비어있을 수 없습니다.")
        @Size(min = 3, max = 3, message = "이해 확인 문항 3개에 모두 답변해야 합니다.")
        List<@Valid SelfCheckItemRequest> checkItems
) {
}
