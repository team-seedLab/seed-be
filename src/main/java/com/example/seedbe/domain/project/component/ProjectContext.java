package com.example.seedbe.domain.project.component;

import java.util.Map;

public final class ProjectContext {
    public static final String VERSION_KEY = "contextVersion";
    public static final String RAW_DOCUMENT_VERSION = "RAW_DOCUMENT_V1";
    public static final String SOURCE_TEXT_KEY = "sourceText";

    private ProjectContext() {
    }

    public static Map<String, Object> rawDocument(String sourceText) {
        return Map.of(
                VERSION_KEY, RAW_DOCUMENT_VERSION,
                SOURCE_TEXT_KEY, sourceText == null ? "" : sourceText
        );
    }

    public static boolean isRawDocument(Map<String, Object> context) {
        return context != null
                && RAW_DOCUMENT_VERSION.equals(context.get(VERSION_KEY));
    }

    public static String sourceText(Map<String, Object> context) {
        if (!isRawDocument(context)) {
            return "";
        }
        Object sourceText = context.get(SOURCE_TEXT_KEY);
        return sourceText == null ? "" : String.valueOf(sourceText);
    }
}
