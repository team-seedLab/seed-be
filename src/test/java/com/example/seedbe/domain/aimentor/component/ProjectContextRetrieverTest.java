package com.example.seedbe.domain.aimentor.component;

import com.example.seedbe.domain.project.component.ProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectContextRetrieverTest {
    private final ProjectContextRetriever retriever = new ProjectContextRetriever();

    @Test
    void selectsChunksRelatedToQuestion() {
        String unrelated = "발표 시간은 십 분이며 슬라이드는 열 장이다. ".repeat(80);
        String related = "평가 기준은 근거의 신뢰성과 인용 형식 준수 여부이다. ".repeat(40);
        Map<String, Object> context = ProjectContext.rawDocument(unrelated + related + unrelated);

        String result = retriever.retrieve(context, "평가 기준과 인용 형식을 알려줘");

        assertThat(result).contains("평가 기준", "인용 형식");
        assertThat(result.length()).isLessThanOrEqualTo(4 * 1_200 * 2);
    }

    @Test
    void flattensLegacyContextWithoutMigration() {
        String result = retriever.retrieve(
                Map.of("topic", "테스트 자동화", "audience", "전공 교수"), "질문");

        assertThat(result).contains("topic: 테스트 자동화", "audience: 전공 교수");
    }
}
