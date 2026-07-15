package com.example.seedbe.domain.aimentor.component;

import com.example.seedbe.domain.aimentor.component.ProjectDocumentChunker.DocumentChunk;
import com.example.seedbe.domain.project.component.ProjectContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ProjectContextRetriever {
    private static final int MAX_SELECTED_CHUNKS = 4;
    private static final int MAX_CHUNKS_PER_PAGE = 2;
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> STOP_WORDS = Set.of(
            "그리고", "하지만", "대한", "관련", "어떻게", "무엇", "이것", "저것", "해주세요", "합니다",
            "현재", "단계", "질문", "답변", "과제", "내용", "작성", "사용", "다음", "참고", "기반"
    );
    private static final List<String> KOREAN_POSTPOSITIONS = List.of(
            "에서", "으로", "에게", "까지", "부터", "처럼", "보다", "은", "는", "이", "가",
            "을", "를", "과", "와", "의", "에", "로", "도", "만"
    );

    private final ProjectDocumentChunker documentChunker;

    public String retrieve(Map<String, Object> initialContext, RetrievalQuery query) {
        if (!ProjectContext.isRawDocument(initialContext)) {
            return legacyContext(initialContext);
        }

        String sourceText = ProjectContext.sourceText(initialContext);
        if (sourceText.isBlank()) {
            return "";
        }

        List<DocumentChunk> chunks = documentChunker.createChunks(sourceText);
        Map<String, Integer> keywordWeights = keywordWeights(query);
        if (chunks.isEmpty() || keywordWeights.isEmpty()) {
            return "";
        }

        Map<String, Integer> documentFrequency = documentFrequency(chunks, keywordWeights.keySet());
        List<ScoredChunk> rankedChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        score(chunk.content(), chunks.size(), keywordWeights, documentFrequency)))
                .filter(chunk -> chunk.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed()
                        .thenComparingInt(chunk -> chunk.chunk().sequence()))
                .toList();

        List<DocumentChunk> selected = selectDiverseChunks(rankedChunks);
        selected.sort(Comparator.comparingInt(DocumentChunk::documentNumber)
                .thenComparingInt(DocumentChunk::pageNumber)
                .thenComparingInt(DocumentChunk::chunkOrder));
        return format(selected);
    }

    private Map<String, Integer> keywordWeights(RetrievalQuery query) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        if (query == null) {
            return weights;
        }
        addKeywords(weights, query.question(), 5, 60);
        addKeywords(weights, query.stepDescription(), 2, 20);
        addKeywords(weights, query.keyFocus(), 2, 30);
        addKeywords(weights, query.requiredElements(), 2, 30);
        return weights;
    }

    private void addKeywords(Map<String, Integer> weights, String text, int weight, int limit) {
        int count = 0;
        for (String token : tokenize(text)) {
            weights.merge(token, weight, Math::max);
            if (++count >= limit) {
                break;
            }
        }
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String rawToken : TOKEN_SEPARATOR.split(text.toLowerCase(Locale.ROOT))) {
            String token = stripKoreanPostposition(rawToken);
            if (token.length() >= 2 && token.length() <= 40 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String stripKoreanPostposition(String token) {
        for (String postposition : KOREAN_POSTPOSITIONS) {
            if (token.endsWith(postposition) && token.length() - postposition.length() >= 2) {
                return token.substring(0, token.length() - postposition.length());
            }
        }
        return token;
    }

    private Map<String, Integer> documentFrequency(List<DocumentChunk> chunks, Set<String> keywords) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            String normalized = chunk.content().toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (normalized.contains(keyword)) {
                    frequencies.merge(keyword, 1, Integer::sum);
                }
            }
        }
        return frequencies;
    }

    private double score(String content, int chunkCount, Map<String, Integer> keywordWeights,
                         Map<String, Integer> documentFrequency) {
        String normalized = content.toLowerCase(Locale.ROOT);
        double score = 0;
        for (Map.Entry<String, Integer> entry : keywordWeights.entrySet()) {
            int termFrequency = Math.min(countOccurrences(normalized, entry.getKey()), 3);
            if (termFrequency == 0) {
                continue;
            }
            int frequency = documentFrequency.getOrDefault(entry.getKey(), chunkCount);
            double inverseDocumentFrequency = Math.log((chunkCount + 1.0) / (frequency + 1.0)) + 1.0;
            score += entry.getValue() * inverseDocumentFrequency * (1.0 + Math.log(termFrequency));
        }
        return score;
    }

    private int countOccurrences(String content, String keyword) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = content.indexOf(keyword, fromIndex)) >= 0) {
            count++;
            fromIndex += keyword.length();
        }
        return count;
    }

    private List<DocumentChunk> selectDiverseChunks(List<ScoredChunk> rankedChunks) {
        List<DocumentChunk> selected = new ArrayList<>();
        Map<PageKey, Integer> countByPage = new HashMap<>();
        Set<String> selectedContents = new LinkedHashSet<>();
        for (ScoredChunk scoredChunk : rankedChunks) {
            DocumentChunk chunk = scoredChunk.chunk();
            PageKey pageKey = new PageKey(chunk.documentNumber(), chunk.pageNumber());
            String normalizedContent = WHITESPACE.matcher(chunk.content()).replaceAll(" ").trim();
            if (countByPage.getOrDefault(pageKey, 0) >= MAX_CHUNKS_PER_PAGE
                    || !selectedContents.add(normalizedContent)) {
                continue;
            }
            selected.add(chunk);
            countByPage.merge(pageKey, 1, Integer::sum);
            if (selected.size() >= MAX_SELECTED_CHUNKS) {
                break;
            }
        }
        return selected;
    }

    private String format(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        PageKey previousPage = null;
        for (DocumentChunk chunk : chunks) {
            PageKey currentPage = new PageKey(chunk.documentNumber(), chunk.pageNumber());
            if (!currentPage.equals(previousPage)) {
                if (!result.isEmpty()) {
                    result.append("\n\n");
                }
                result.append("[문서 ").append(chunk.documentNumber())
                        .append(", 페이지 ").append(chunk.pageNumber()).append("]\n");
                previousPage = currentPage;
            } else {
                result.append('\n');
            }
            result.append(chunk.content());
        }
        return result.toString();
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

    public record RetrievalQuery(
            String question,
            String stepDescription,
            String keyFocus,
            String requiredElements
    ) {
    }

    private record ScoredChunk(DocumentChunk chunk, double score) {
    }

    private record PageKey(int documentNumber, int pageNumber) {
    }
}
