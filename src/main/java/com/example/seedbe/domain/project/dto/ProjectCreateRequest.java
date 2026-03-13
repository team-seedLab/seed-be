package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.enums.RoadmapType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public record ProjectCreateRequest(
        @Schema(description = "프로젝트 제목", example = "소프트웨어 공학 중간고사 요약")
        String title,

        @Schema(description = "로드맵 과제 유형", example = "EXAM_STUDY_SUMMARY")
        RoadmapType roadmapType,

        @Schema(description = "유저의 추가 요구사항 (최대 1000줄)", example = "4가지 테스트 활동 부분만 표 형태로 요약해 줘.")
        @NotBlank
        String userIntent,

        @Schema(description = "분석할 과제 PDF 파일들 (최대 3개)")
        @NotNull
        List<MultipartFile> files
) {
}