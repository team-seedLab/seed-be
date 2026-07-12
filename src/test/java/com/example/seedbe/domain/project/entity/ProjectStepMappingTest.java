package com.example.seedbe.domain.project.entity;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectStepMappingTest {
    @Test
    void projectStepHasProjectRoadmapStepUniqueConstraint() {
        Table table = ProjectStep.class.getAnnotation(Table.class);

        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .anyMatch(columns -> Arrays.equals(columns, new String[]{"project_id", "roadmap_step"})))
                .isTrue();
    }
}
