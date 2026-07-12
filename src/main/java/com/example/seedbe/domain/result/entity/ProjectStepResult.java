package com.example.seedbe.domain.result.entity;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "project_step_results", uniqueConstraints =
        @UniqueConstraint(name = "uk_project_step_results_step", columnNames = "step_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectStepResult extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_result_id", nullable = false, updatable = false)
    private UUID stepResultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private ProjectStep step;

    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    @Builder
    public ProjectStepResult(ProjectStep step, String contentMarkdown) {
        this.step = step;
        this.contentMarkdown = contentMarkdown;
    }

    public void updateContent(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }
}
