package com.example.seedbe.domain.project.component.pdf;

import org.springframework.stereotype.Component;

@Component
public class PdfTextNormalizer {
    private static final String HORIZONTAL_WHITESPACE = "[ \\t]+";
    private static final String VERTICAL_WHITESPACE = "(\\r\\n|\\r|\\n)+";

    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        // 줄바꿈은 문단/목록 구조이므로 유지하고, 같은 줄 안의 과도한 공백만 줄인다.
        return text.replaceAll(HORIZONTAL_WHITESPACE, " ")
                .replaceAll(VERTICAL_WHITESPACE, "\n")
                .trim();
    }

    public String normalizeAndLimit(String text, int maxLength) {
        String normalized = normalize(text);
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        int endIndex = maxLength;
        if (endIndex > 0
                && Character.isHighSurrogate(normalized.charAt(endIndex - 1))
                && Character.isLowSurrogate(normalized.charAt(endIndex))) {
            endIndex--;
        }
        return normalized.substring(0, endIndex);
    }
}
