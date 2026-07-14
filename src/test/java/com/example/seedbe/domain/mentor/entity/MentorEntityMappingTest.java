package com.example.seedbe.domain.mentor.entity;

import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MentorEntityMappingTest {
    @Test
    void mapsMentorStudentPairAsUnique() {
        Table table = MentorStudent.class.getAnnotation(Table.class);

        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .anyMatch(columns -> Arrays.equals(columns, new String[]{"mentor_id", "student_id"})))
                .isTrue();
    }

    @Test
    void mapsProjectMentorReviewAsUniqueAndTracksReviewedAt() {
        Table table = ProjectReview.class.getAnnotation(Table.class);
        ProjectReview review = ProjectReview.builder().build();

        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .anyMatch(columns -> Arrays.equals(columns, new String[]{"project_id", "mentor_id"})))
                .isTrue();
        assertThat(review.getStatus()).isEqualTo(ProjectReviewStatus.REVIEWING);
        assertThat(review.getReviewedAt()).isNull();

        review.markReviewed();

        assertThat(review.getStatus()).isEqualTo(ProjectReviewStatus.REVIEWED);
        assertThat(review.getReviewedAt()).isNotNull();
    }
}
