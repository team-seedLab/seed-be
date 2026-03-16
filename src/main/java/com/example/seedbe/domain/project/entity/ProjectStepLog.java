package com.example.seedbe.domain.project.entity;

import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "project_step_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectStepLog extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "project_step_log_id", nullable = false, updatable = false)
    private UUID projectStepLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id",  nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id",  nullable = false)
    private PromptTemplate promptTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_step", nullable = false, length = 50)
    private RoadmapStep roadmapStep;

    @Column(name = "provided_prompt_snapshot", columnDefinition = "TEXT", nullable = false)
    private String providedPromptSnapshot;

    @Column(name = "user_submitted_result", columnDefinition = "TEXT", nullable = false)
    private String userSubmittedResult;
}
