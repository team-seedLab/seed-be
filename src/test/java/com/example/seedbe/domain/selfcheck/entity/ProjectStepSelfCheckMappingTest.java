package com.example.seedbe.domain.selfcheck.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectStepSelfCheckMappingTest {
    @Test
    void mapsUniqueStepAndJsonbItems() throws NoSuchFieldException {
        Table table = ProjectStepSelfCheck.class.getAnnotation(Table.class);
        Column column = ProjectStepSelfCheck.class.getDeclaredField("checkItems").getAnnotation(Column.class);

        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .anyMatch(columns -> Arrays.equals(columns, new String[]{"step_id"})))
                .isTrue();
        assertThat(column.columnDefinition()).isEqualTo("jsonb");
        assertThat(ProjectStepSelfCheck.class.getDeclaredField("checkItems")
                .isAnnotationPresent(JdbcTypeCode.class)).isTrue();
    }
}
