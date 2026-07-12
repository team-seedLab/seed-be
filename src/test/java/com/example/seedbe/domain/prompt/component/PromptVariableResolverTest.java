package com.example.seedbe.domain.prompt.component;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptVariableResolverTest {
    private final PromptVariableResolver resolver = new PromptVariableResolver();

    @Test
    void resolvesOriginalTemplateInSinglePass() {
        String result = resolver.resolve("[topic] / [audience]", Map.of(
                "topic", "[audience]를 위한 AI",
                "audience", "대학생"
        ));

        assertThat(result).isEqualTo("[audience]를 위한 AI / 대학생");
    }

    @Test
    void preservesDollarAndBackslashInReplacementValue() {
        String result = resolver.resolve("경로: [path]", Map.of("path", "$HOME\\seed"));

        assertThat(result).isEqualTo("경로: $HOME\\seed");
    }

    @Test
    void replacesNullWithEmptyAndKeepsUnknownPlaceholder() {
        Map<String, Object> context = new HashMap<>();
        context.put("length", null);

        String result = resolver.resolve("분량: [length], 대상: [audience]", context);

        assertThat(result).isEqualTo("분량: , 대상: [audience]");
    }
}
