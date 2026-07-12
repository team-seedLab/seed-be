package com.example.seedbe.domain.prompt.component;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PromptDiffCalculator {
    private static final String DIFF_VERSION = "PREFIX_SUFFIX_V1";

    public PromptDiff calculate(String original, String edited) {
        int[] originalCodePoints = original.codePoints().toArray();
        int[] editedCodePoints = edited.codePoints().toArray();
        int prefixLength = commonPrefixLength(originalCodePoints, editedCodePoints);
        int suffixLength = commonSuffixLength(originalCodePoints, editedCodePoints, prefixLength);

        int removedCount = originalCodePoints.length - prefixLength - suffixLength;
        int addedCount = editedCodePoints.length - prefixLength - suffixLength;
        List<Map<String, String>> segments = new ArrayList<>();

        addSegment(segments, "EQUAL", toString(originalCodePoints, 0, prefixLength));
        addSegment(segments, "REMOVED", toString(originalCodePoints, prefixLength, removedCount));
        addSegment(segments, "ADDED", toString(editedCodePoints, prefixLength, addedCount));
        addSegment(segments, "EQUAL", toString(originalCodePoints,
                originalCodePoints.length - suffixLength, suffixLength));

        Map<String, Object> diffJson = new LinkedHashMap<>();
        diffJson.put("version", DIFF_VERSION);
        diffJson.put("segments", segments);
        return new PromptDiff(addedCount, removedCount, diffJson);
    }

    private int commonPrefixLength(int[] original, int[] edited) {
        int limit = Math.min(original.length, edited.length);
        int index = 0;
        while (index < limit && original[index] == edited[index]) {
            index++;
        }
        return index;
    }

    private int commonSuffixLength(int[] original, int[] edited, int prefixLength) {
        int maxSuffixLength = Math.min(original.length, edited.length) - prefixLength;
        int length = 0;
        while (length < maxSuffixLength
                && original[original.length - 1 - length] == edited[edited.length - 1 - length]) {
            length++;
        }
        return length;
    }

    private String toString(int[] codePoints, int offset, int count) {
        return count == 0 ? "" : new String(codePoints, offset, count);
    }

    private void addSegment(List<Map<String, String>> segments, String type, String text) {
        if (!text.isEmpty()) {
            segments.add(Map.of("type", type, "text", text));
        }
    }

    public record PromptDiff(int addedCount, int removedCount, Map<String, Object> diffJson) {
    }
}
