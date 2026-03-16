package com.example.seedbe.domain.prompt.entity;

import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prompt_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prompt_template_id", updatable = false, nullable = false)
    private UUID promptTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_type", nullable = false, length = 30)
    private RoadmapType roadmapType;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_step", nullable = false, length = 50)
    private RoadmapStep roadmapStep;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "action_prompt", columnDefinition = "TEXT", nullable = false)
    private String actionPrompt;

    @Column(name = "format_prompt", columnDefinition = "TEXT", nullable = false)
    private String formatPrompt;

    // PostgreSQL의 JSONB 컬럼 매핑 (필요 변수 목록)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_vars", columnDefinition = "jsonb")
    private List<String> expectedVars;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
