package com.example.seedbe.domain.project.entity;

import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapType;
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
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "roadmap_type", nullable = false)
    private RoadmapType roadmapType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status;

    // PostgreSQL의 jsonb 타입을 Java의 Map이나 커스텀 객체로 바로 맵핑
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initial_context", columnDefinition = "jsonb")
    private Map<String, Object> initialContext;

    @Builder
    public Project(UUID userId, String title, RoadmapType roadmapType, ProjectStatus status, Map<String, Object> initialContext) {
        this.userId = userId;
        this.title = title;
        this.roadmapType = roadmapType;
        this.status = status;
        this.initialContext = initialContext;
    }
}
