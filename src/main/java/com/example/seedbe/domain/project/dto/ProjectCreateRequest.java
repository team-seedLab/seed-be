package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.enums.RoadmapType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ProjectCreateRequest(
        @NotBlank(message = "프로젝트 제목은 필수입니다.")
        String title,
        @NotNull(message = "로드맵 유형은 필수입니다.")
        RoadmapType roadmapType,
        @NotNull(message = "초기 컨텍스트 데이터는 필수입니다.")
        Map<String, Object> initialContext
) {

}
