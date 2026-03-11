package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.enums.RoadmapType;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ProjectCreateRequest(
        @NotBlank(message = "프로젝트 제목은 필수입니다.")
        String title,
        @NotBlank(message = "로드맵 유형은 필수입니다.")
        RoadmapType roadmapType,
        Map<String, Object> initialContext
) {

}
