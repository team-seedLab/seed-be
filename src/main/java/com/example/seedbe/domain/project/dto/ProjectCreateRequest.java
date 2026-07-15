package com.example.seedbe.domain.project.dto;

import com.example.seedbe.domain.project.enums.RoadmapType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public record ProjectCreateRequest(
        @Schema(description = "프로젝트 제목", example = "소프트웨어 공학 중간고사 요약")
        String title,

        @Schema(description = "로드맵 과제 유형", example = "EXAM_STUDY_SUMMARY")
        RoadmapType roadmapType,

        @Schema(description = "원하는 결과물", example = "A4 3장 분량의 리포트")
        @Size(max = 2000, message = "원하는 결과물은 2000자를 초과할 수 없습니다.")
        String desiredOutcome,

        @Schema(description = "핵심 관점", example = "소프트웨어 테스트 활동 중심")
        @Size(max = 2000, message = "핵심 관점은 2000자를 초과할 수 없습니다.")
        String keyFocus,

        @Schema(description = "필수 포함 요소", example = "4가지 테스트 활동을 표로 포함")
        @Size(max = 2000, message = "필수 포함 요소는 2000자를 초과할 수 없습니다.")
        String requiredElements,

        @Schema(description = "분석할 과제 PDF 파일들 (최대 2개)")
        List<MultipartFile> files
) {
}
