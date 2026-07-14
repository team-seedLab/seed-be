package com.example.seedbe.domain.selfcheck.entity;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.selfcheck.model.SelfCheckItem;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_step_self_checks", uniqueConstraints =
        @UniqueConstraint(name = "uk_project_step_self_checks_step", columnNames = "step_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectStepSelfCheck extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "self_check_id", nullable = false, updatable = false)
    private UUID selfCheckId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private ProjectStep step;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "check_items_json", nullable = false, columnDefinition = "jsonb")
    private List<SelfCheckItem> checkItems;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Builder
    public ProjectStepSelfCheck(ProjectStep step, List<SelfCheckItem> checkItems) {
        this.step = step;
        this.checkItems = List.copyOf(checkItems);
        this.submittedAt = LocalDateTime.now();
    }

    public void overwrite(List<SelfCheckItem> checkItems) {
        this.checkItems = List.copyOf(checkItems);
        this.submittedAt = LocalDateTime.now();
    }
}
