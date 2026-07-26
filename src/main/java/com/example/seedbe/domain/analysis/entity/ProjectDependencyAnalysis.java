package com.example.seedbe.domain.analysis.entity;

import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "project_dependency_analysis", uniqueConstraints =
        @UniqueConstraint(name = "uk_project_dependency_analysis_project", columnNames = "project_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectDependencyAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dependency_analysis_id", nullable = false, updatable = false)
    private UUID dependencyAnalysisId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "initiative_score", nullable = false)
    private Integer initiativeScore;

    @Column(name = "user_edit_ratio", nullable = false)
    private Integer userEditRatio;

    @Column(name = "ai_dependency_ratio", nullable = false)
    private Integer aiDependencyRatio;

    @Builder
    public ProjectDependencyAnalysis(Project project, Integer initiativeScore, Integer userEditRatio,
                                     Integer aiDependencyRatio) {
        this.project = project;
        update(initiativeScore, userEditRatio, aiDependencyRatio);
    }

    public void update(Integer initiativeScore, Integer userEditRatio, Integer aiDependencyRatio) {
        this.initiativeScore = initiativeScore;
        this.userEditRatio = userEditRatio;
        this.aiDependencyRatio = aiDependencyRatio;
    }
}
