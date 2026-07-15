package com.example.seedbe.domain.aimentor.component;

import com.example.seedbe.domain.project.component.ProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectContextRetrieverTest {
    private final ProjectContextRetriever retriever =
            new ProjectContextRetriever(new ProjectDocumentChunker());

    @Test
    void selectsRelatedPageFromFortyPageDocument() {
        StringBuilder source = new StringBuilder("[문서 1 시작]\n");
        String pageFiller = "수업 일정과 제출 안내를 확인한다. ".repeat(140);
        for (int page = 1; page <= 40; page++) {
            source.append("[페이지 ").append(page).append("]\n")
                    .append(pageFiller)
                    .append('\n');
            if (page == 37) {
                source.append("가설 검정에서는 영가설과 유의수준을 먼저 정하고 검정 통계량을 해석한다.\n");
            }
        }
        source.append("[문서 1 끝]");
        assertThat(source.length()).isGreaterThan(100_000);

        String result = retriever.retrieve(
                ProjectContext.rawDocument(source.toString()),
                query("영가설과 유의수준은 어떻게 정하나요?"));

        assertThat(result)
                .contains("[문서 1, 페이지 37]")
                .contains("영가설", "유의수준");
    }

    @Test
    void returnsEmptyContextWhenNoKeywordMatches() {
        Map<String, Object> context = ProjectContext.rawDocument("""
                [문서 1 시작]
                [페이지 1]
                발표 시간은 십 분이며 슬라이드는 열 장이다.
                [문서 1 끝]
                """);

        String result = retriever.retrieve(context, query("양자 얽힘의 원리를 알려줘"));

        assertThat(result).isEmpty();
    }

    @Test
    void matchesKoreanKeywordsWithDifferentPostpositions() {
        Map<String, Object> context = ProjectContext.rawDocument("""
                [문서 1 시작]
                [페이지 8]
                연구자는 유의수준을 사전에 정해야 한다.
                [문서 1 끝]
                """);

        String result = retriever.retrieve(context, query("유의수준은 어떻게 정하나요?"));

        assertThat(result).contains("[문서 1, 페이지 8]", "유의수준을");
    }

    @Test
    void suppressesRepeatedBoilerplateAndPreservesPageSource() {
        String repeated = "평가 기준 반복 안내 문구입니다.";
        String source = """
                [문서 1 시작]
                [페이지 1]
                %s
                [페이지 2]
                %s
                [페이지 3]
                평가 기준 중 근거의 신뢰성과 인용 형식을 우선 확인한다.
                [문서 1 끝]
                """.formatted(repeated, repeated);

        String result = retriever.retrieve(
                ProjectContext.rawDocument(source),
                query("평가 기준에서 근거 신뢰성과 인용 형식을 알려줘"));

        assertThat(result)
                .contains("[문서 1, 페이지 3]")
                .contains("근거의 신뢰성", "인용 형식")
                .containsOnlyOnce(repeated);
    }

    @Test
    void limitsSelectedContextAndKeepsChunkAtSentenceBoundary() {
        String related = "평가 기준은 근거의 신뢰성과 인용 형식 준수 여부이다. ".repeat(300);
        Map<String, Object> context = ProjectContext.rawDocument("""
                [문서 1 시작]
                [페이지 12]
                %s
                [문서 1 끝]
                """.formatted(related));

        String result = retriever.retrieve(context, query("평가 기준과 인용 형식을 알려줘"));

        assertThat(result).contains("[문서 1, 페이지 12]");
        assertThat(result.length()).isLessThan(5_000);
        assertThat(result).doesNotEndWith("근거의 신뢰성과 인용 형식");
    }

    @Test
    void flattensLegacyContextWithoutMigration() {
        String result = retriever.retrieve(
                Map.of("topic", "테스트 자동화", "audience", "전공 교수"),
                query("질문"));

        assertThat(result).contains("topic: 테스트 자동화", "audience: 전공 교수");
    }

    private ProjectContextRetriever.RetrievalQuery query(String question) {
        return new ProjectContextRetriever.RetrievalQuery(question, "", "", "");
    }
}
