package com.example.seedbe.domain.prompt.component;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptDiffCalculatorTest {
    private final PromptDiffCalculator calculator = new PromptDiffCalculator();

    @Test
    void calculatesUnicodeCodePointCountsAndReconstructableSegments() {
        PromptDiffCalculator.PromptDiff diff = calculator.calculate("A😀B", "A한글B");

        assertThat(diff.removedCount()).isEqualTo(1);
        assertThat(diff.addedCount()).isEqualTo(2);
        assertThat(diff.diffJson()).containsEntry("version", "PREFIX_SUFFIX_V1");
        List<Map<String, String>> segments = (List<Map<String, String>>) diff.diffJson().get("segments");
        assertThat(segments).containsExactly(
                Map.of("type", "EQUAL", "text", "A"),
                Map.of("type", "REMOVED", "text", "😀"),
                Map.of("type", "ADDED", "text", "한글"),
                Map.of("type", "EQUAL", "text", "B")
        );
    }
}
