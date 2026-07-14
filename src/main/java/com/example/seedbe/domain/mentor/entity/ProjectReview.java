package com.example.seedbe.domain.mentor.entity;

import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_reviews", uniqueConstraints =
        @UniqueConstraint(name = "uk_project_reviews_project_mentor", columnNames = {"project_id", "mentor_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectReview extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "project_review_id", nullable = false, updatable = false)
    private UUID projectReviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectReviewStatus status;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public ProjectReview(Project project, User mentor, ProjectReviewStatus status) {
        this.project = project;
        this.mentor = mentor;
        this.status = status == null ? ProjectReviewStatus.REVIEWING : status;
        this.reviewedAt = this.status == ProjectReviewStatus.REVIEWED ? LocalDateTime.now() : null;
    }

    public void markReviewed() {
        this.status = ProjectReviewStatus.REVIEWED;
        this.reviewedAt = LocalDateTime.now();
    }
}
