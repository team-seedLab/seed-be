package com.example.seedbe.domain.test;

import com.example.seedbe.global.common.response.ApiResponse;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // 1. 정상 성공 케이스
    @GetMapping("/success")
    public ApiResponse<String> success() {
        return ApiResponse.success("성공 데이터입니다!");
    }

    // 2. 비즈니스 예외 발생 케이스 (예: 프로젝트를 못 찾았을 때)
    @GetMapping("/business-error")
    public ApiResponse<Void> businessError() {
        throw new BusinessException(ErrorType.PROJECT_NOT_FOUND);
    }

    // 3. 서버 내부 에러 발생 케이스 (예: 널포인터 에러)
    @GetMapping("/server-error")
    public ApiResponse<Void> serverError() {
        throw new RuntimeException("DB 커넥션이 끊어졌습니다!"); // 알 수 없는 에러 가정
    }

    // 4. 유효성 검사 실패 케이스
    @PostMapping("/validation-error")
    public ApiResponse<String> validationError(@Valid @RequestBody TestDto request) {
        return ApiResponse.success("검증 통과!");
    }

    @Getter
    public static class TestDto {
        @NotBlank(message = "이름은 필수입니다.")
        private String name;
    }
}
