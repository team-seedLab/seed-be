package com.example.seedbe.domain.selfcheck.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SelfCheckQuestionType {
    CORE_UNDERSTANDING(
            "core_understanding",
            "이번 단계에서 이해한 핵심 내용을 자신의 말로 설명해 주세요."
    ),
    RESULT_APPLICATION(
            "result_application",
            "이해한 내용을 결과물에 어떻게 적용했으며, 그렇게 적용한 이유는 무엇인가요?"
    ),
    UNCERTAINTY_REVIEW(
            "uncertainty_review",
            "아직 확실하지 않거나 다시 확인해야 할 부분은 무엇인가요? 없다면 실수하기 쉬운 부분을 작성해 주세요."
    );

    private final String key;
    private final String question;
}
