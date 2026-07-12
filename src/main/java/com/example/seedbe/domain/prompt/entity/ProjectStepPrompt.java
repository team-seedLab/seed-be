package com.example.seedbe.domain.prompt.entity;

import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "project_step_prompts", uniqueConstraints =
        @UniqueConstraint(name = "uk_project_step_prompts_step", columnNames = "step_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectStepPrompt extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_prompt_id", nullable = false, updatable = false)
    private UUID stepPromptId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private ProjectStep step;

    @Column(name = "provided_prompt_snapshot", nullable = false, columnDefinition = "TEXT")
    private String providedPromptSnapshot;

    @Column(name = "edited_prompt", columnDefinition = "TEXT")
    private String editedPrompt;

    @Column(name = "added_count", nullable = false)
    private Integer addedCount;

    @Column(name = "removed_count", nullable = false)
    private Integer removedCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_json", columnDefinition = "jsonb")
    private Map<String, Object> diffJson;

    @Builder
    public ProjectStepPrompt(ProjectStep step, String providedPromptSnapshot, String editedPrompt,
                             Integer addedCount, Integer removedCount, Map<String, Object> diffJson) {
        this.step = step;
        this.providedPromptSnapshot = providedPromptSnapshot;
        this.editedPrompt = editedPrompt;
        this.addedCount = addedCount == null ? 0 : addedCount;
        this.removedCount = removedCount == null ? 0 : removedCount;
        this.diffJson = diffJson;
    }

    public void updateEditedPrompt(String editedPrompt, int addedCount, int removedCount,
                                   Map<String, Object> diffJson) {
        this.editedPrompt = editedPrompt;
        this.addedCount = addedCount;
        this.removedCount = removedCount;
        this.diffJson = diffJson;
    }
}
