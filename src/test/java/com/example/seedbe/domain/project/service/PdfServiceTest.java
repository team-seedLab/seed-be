package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.pdf.PdfTextNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfServiceTest {

    private final PdfTextNormalizer textNormalizer = new PdfTextNormalizer();

    @Test
    @DisplayName("PDF 텍스트 정규화 시 줄바꿈은 유지하고 가로 공백만 줄인다.")
    void normalizePreservesLineBreaksAndNormalizesHorizontalWhitespace() {
        String text = """
                first    line
                second\t\tline

                third      line
                """;

        String normalizedText = textNormalizer.normalize(text);

        assertThat(normalizedText).contains("first line\nsecond line");
        assertThat(normalizedText).contains("\nthird line");
        assertThat(normalizedText).doesNotContain("first line second line");
        assertThat(normalizedText).doesNotContain("    ");
    }
}
