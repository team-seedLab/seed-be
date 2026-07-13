package com.example.seedbe.domain.aimentor.component;

import com.example.seedbe.domain.project.component.ProjectContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ProjectContextRetriever {
    private static final int CHUNK_SIZE = 1_200;
    private static final int CHUNK_OVERLAP = 200;
    private static final int MAX_SELECTED_CHUNKS = 4;
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "그리고", "하지만", "대한", "관련", "어떻게", "무엇", "이것", "저것", "해주세요", "합니다"
    );

    public String retrieve(Map<String, Object> initialContext, String query) {
        if (!ProjectContext.isRawDocument(initialContext)) {
            return legacyContext(initialContext);
        }

        String sourceText = ProjectContext.sourceText(initialContext);
        if (sourceText.isBlank()) {
            return "";
        }

        List<String> chunks = chunk(sourceText);
        Set<String> keywords = keywords(query);
        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            scoredChunks.add(new ScoredChunk(index, chunks.get(index), score(chunks.get(index), keywords)));
        }

        List<ScoredChunk> selected = scoredChunks.stream()
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed()
                        .thenComparingInt(ScoredChunk::index))
                .limit(MAX_SELECTED_CHUNKS)
                .sorted(Comparator.comparingInt(ScoredChunk::index))
                .toList();
        return selected.stream()
                .map(ScoredChunk::text)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private List<String> chunk(String text) {
        int[] codePoints = text.codePoints().toArray();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < codePoints.length) {
            int length = Math.min(CHUNK_SIZE, codePoints.length - start);
            chunks.add(new String(codePoints, start, length));
            if (start + length >= codePoints.length) {
                break;
            }
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private Set<String> keywords(String query) {
        Set<String> keywords = new LinkedHashSet<>();
        if (query == null || query.isBlank()) {
            return keywords;
        }
        for (String token : TOKEN_SEPARATOR.split(query.toLowerCase())) {
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private int score(String chunk, Set<String> keywords) {
        String normalizedChunk = chunk.toLowerCase();
        int score = 0;
        for (String keyword : keywords) {
            int fromIndex = 0;
            while ((fromIndex = normalizedChunk.indexOf(keyword, fromIndex)) >= 0) {
                score++;
                fromIndex += keyword.length();
            }
        }
        return score;
    }

    private String legacyContext(Map<String, Object> initialContext) {
        if (initialContext == null || initialContext.isEmpty()) {
            return "";
        }
        return initialContext.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private record ScoredChunk(int index, String text, int score) {
    }
}
