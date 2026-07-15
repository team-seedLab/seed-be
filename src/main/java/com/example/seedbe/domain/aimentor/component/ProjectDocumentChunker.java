package com.example.seedbe.domain.aimentor.component;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProjectDocumentChunker {
    private static final int MAX_CHUNK_CODE_POINTS = 1_200;
    private static final int MIN_CHUNK_CODE_POINTS = 600;
    private static final Pattern DOCUMENT_START = Pattern.compile("^\\[문서 (\\d+) 시작]$");
    private static final Pattern DOCUMENT_END = Pattern.compile("^\\[문서 \\d+ 끝]$");
    private static final Pattern PAGE_MARKER = Pattern.compile("^\\[페이지 (\\d+)]$");

    List<DocumentChunk> createChunks(String sourceText) {
        List<DocumentPage> pages = parsePages(sourceText);
        List<DocumentChunk> chunks = new ArrayList<>();
        int sequence = 0;
        for (DocumentPage page : pages) {
            List<String> pageChunks = splitPage(page.content());
            for (int chunkOrder = 0; chunkOrder < pageChunks.size(); chunkOrder++) {
                chunks.add(new DocumentChunk(
                        page.documentNumber(), page.pageNumber(), chunkOrder, sequence++, pageChunks.get(chunkOrder)));
            }
        }
        return chunks;
    }

    private List<DocumentPage> parsePages(String sourceText) {
        List<DocumentPage> pages = new ArrayList<>();
        StringBuilder pageText = new StringBuilder();
        int documentNumber = 1;
        int pageNumber = 1;

        for (String rawLine : sourceText.lines().toList()) {
            String line = rawLine.trim();
            Matcher documentMatcher = DOCUMENT_START.matcher(line);
            Matcher pageMatcher = PAGE_MARKER.matcher(line);
            if (documentMatcher.matches()) {
                addPage(pages, documentNumber, pageNumber, pageText);
                documentNumber = Integer.parseInt(documentMatcher.group(1));
                pageNumber = 1;
                continue;
            }
            if (pageMatcher.matches()) {
                addPage(pages, documentNumber, pageNumber, pageText);
                pageNumber = Integer.parseInt(pageMatcher.group(1));
                continue;
            }
            if (DOCUMENT_END.matcher(line).matches()) {
                addPage(pages, documentNumber, pageNumber, pageText);
                continue;
            }
            if (!line.isBlank()) {
                if (!pageText.isEmpty()) {
                    pageText.append('\n');
                }
                pageText.append(line);
            }
        }
        addPage(pages, documentNumber, pageNumber, pageText);
        return pages;
    }

    private void addPage(List<DocumentPage> pages, int documentNumber, int pageNumber,
                         StringBuilder pageText) {
        String content = pageText.toString().trim();
        if (!content.isBlank()) {
            pages.add(new DocumentPage(documentNumber, pageNumber, content));
        }
        pageText.setLength(0);
    }

    private List<String> splitPage(String content) {
        int[] codePoints = content.codePoints().toArray();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < codePoints.length) {
            int end = Math.min(start + MAX_CHUNK_CODE_POINTS, codePoints.length);
            if (end < codePoints.length) {
                end = findBoundary(codePoints, start, end);
            }
            String chunk = new String(codePoints, start, end - start).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            start = end;
        }
        return chunks;
    }

    private int findBoundary(int[] codePoints, int start, int end) {
        int minimum = Math.min(start + MIN_CHUNK_CODE_POINTS, end);
        for (int index = end; index > minimum; index--) {
            int codePoint = codePoints[index - 1];
            if (codePoint == '\n' || codePoint == '.' || codePoint == '?' || codePoint == '!'
                    || codePoint == '。' || codePoint == '？' || codePoint == '！') {
                return index;
            }
        }
        for (int index = end; index > minimum; index--) {
            if (Character.isWhitespace(codePoints[index - 1])) {
                return index;
            }
        }
        return end;
    }

    record DocumentChunk(int documentNumber, int pageNumber, int chunkOrder,
                         int sequence, String content) {
    }

    private record DocumentPage(int documentNumber, int pageNumber, String content) {
    }
}
