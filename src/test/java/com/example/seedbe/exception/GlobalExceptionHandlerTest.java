package com.example.seedbe.exception;

import com.example.seedbe.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("1. 정상 요청 시 SUCCESS 상태와 데이터가 반환된다.")
    void successTest() throws Exception {
        mockMvc.perform(get("/api/test/success"))
                .andDo(print()) // 콘솔에 결과 출력 (눈으로 확인용)
                .andExpect(status().isOk()) // HTTP 200 검증
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("성공 데이터입니다!"));
    }

    @Test
    @DisplayName("2. 비즈니스 예외 발생 시 FAIL 상태와 정의된 ErrorType이 반환된다.")
    void businessExceptionTest() throws Exception {
        mockMvc.perform(get("/api/test/business-error"))
                .andDo(print())
                .andExpect(status().isNotFound()) // ErrorType.PROJECT_NOT_FOUND의 404 검증
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errorCode").value("P001"))
                .andExpect(jsonPath("$.errorMessage").value("해당 프로젝트를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("3. 처리하지 못한 예외 발생 시 ERROR 상태와 500 에러가 반환된다.")
    void serverExceptionTest() throws Exception {
        mockMvc.perform(get("/api/test/server-error"))
                .andDo(print())
                .andExpect(status().isInternalServerError()) // 500 검증
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("G002"))
                .andExpect(jsonPath("$.errorMessage").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("4. @Valid 유효성 검사 실패 시 FAIL 상태와 400 에러가 반환된다.")
    void validationExceptionTest() throws Exception {
        // name을 일부러 비워서(null) 전송
        TestController.TestDto request = new TestController.TestDto();
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isBadRequest()) // 400 검증
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errorCode").value("G001"))
                .andExpect(jsonPath("$.errorMessage").value("이름은 필수입니다."));
    }
}
