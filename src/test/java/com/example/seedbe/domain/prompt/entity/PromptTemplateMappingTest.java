package com.example.seedbe.domain.prompt.entity;

import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateMappingTest {
    @Test
    void matchesPromptTemplateSchemaLengthsAndTimestamps() throws Exception {
        assertThat(BaseTimeEntity.class.isAssignableFrom(PromptTemplate.class)).isTrue();
        assertThat(columnLength("roadmapType")).isEqualTo(50);
        assertThat(columnLength("roadmapStep")).isEqualTo(80);
        assertThat(columnLength("version")).isEqualTo(30);
    }

    private int columnLength(String fieldName) throws Exception {
        return PromptTemplate.class.getDeclaredField(fieldName).getAnnotation(Column.class).length();
    }
}
