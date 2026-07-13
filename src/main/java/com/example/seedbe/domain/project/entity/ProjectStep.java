package com.example.seedbe.domain.project.entity;

import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "project_steps",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_steps_project_roadmap_step", columnNames = {"project_id", "roadmap_step"}),
                @UniqueConstraint(name = "uk_project_steps_project_step_order", columnNames = {"project_id", "step_order"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectStep extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id", nullable = false)
    private PromptTemplate promptTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_step", nullable = false, length = 80)
    private RoadmapStep roadmapStep;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectStepStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public ProjectStep(Project project, PromptTemplate promptTemplate, RoadmapStep roadmapStep,
                       Integer stepOrder, ProjectStepStatus status) {
        this.project = project;
        this.promptTemplate = promptTemplate;
        this.roadmapStep = roadmapStep;
        this.stepOrder = stepOrder;
        this.status = status == null ? ProjectStepStatus.PENDING : status;
    }

    public void start() {
        if (status == ProjectStepStatus.PENDING) {
            status = ProjectStepStatus.IN_PROGRESS;
        }
    }

    public void complete() {
        this.status = ProjectStepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
