package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper =  new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public Map<String, Object> analyzeToJSON(String pdfText, String userIntent, RoadmapType roadmapType) {
        // 템플릿 엔진용 프롬프트 생성
        String prompt = buildPrompt(pdfText, userIntent, roadmapType);

        // Gemini API 실제 호출
        String aiResponseJson = callGeminiApi(prompt);

        // 응답 텍스트를 Map으로 파싱
        return parseJsonToMap(aiResponseJson);
    }

    private String buildPrompt(String pdfText, String userIntent, RoadmapType type) {
        return String.format("""
                너는 대학생의 과제를 분석하여, 또 다른 AI(챗GPT 등)에게 지시를 내릴 '완벽한 프롬프트 변수'를 추출하는 최고 수준의 프롬프트 엔지니어 AI야.
                아래 [과제 텍스트]와 [유저 요구사항]을 분석하여, '%s' 과제 수행에 필요한 4가지 핵심 변수를 추출해 줘.
                
                [과제 텍스트]
                %s
                
                [유저 요구사항]
                %s
                
                [매우 중요한 행동 원칙]
                너는 과제 내용을 직접 요약하거나 내용을 편집하는 역할이 절대 아니야!
                원본 텍스트에 있는 중요한 전공 지식이나 세부 개념(예: 특정 이론, 도표 내용 등)이 최종 결과물에서 임의로 누락되지 않도록 방어적인 지침을 작성해야 해.
                
                반드시 아래의 JSON Key를 포함하여 순수 JSON 포맷으로만 응답해. 마크다운(```json)이나 불필요한 텍스트는 절대 쓰지 마.
                {
                  "Topic": "과제의 전체 범위를 포괄하는 구체적인 핵심 주제 (단어가 아닌, '무엇을 어떻게 다루어야 하는지' 명확한 맥락을 포함할 것)",
                  "Role": "이 과제를 완벽하게 수행하기 위해 빙의해야 할 최고의 전문가 페르소나 (예: 15년 차 소프트웨어 테스트 엔지니어, 깐깐한 전공 교수)",
                  "Task_Type": "최종 결과물의 구체적인 포맷과 분량 (예: A4 3장 분량의 개조식 요약본, 시각화된 마크다운 표)",
                  "Constraint": "작성 시 지켜야 할 '형식적 규칙'과 '필수 반영 요소'. (주의: 원본의 목차와 강조점을 지시하되, '내용을 요약/축약하라'는 말 대신 '원본의 디테일과 핵심 개념을 누락 없이 꼼꼼히 반영할 것'을 강력하게 강제할 것)"
                }
                """,
                type.name(),
                pdfText,
                userIntent != null ? userIntent : "특별한 요구사항 없음"
        );
    }

    private String callGeminiApi(String prompt) {
        try {
            // Map 객체를 만들어서 ObjectMapper에게 JSON 변환
            Map<String, Object> requestBodyMap = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            // Map -> 안전한 JSON String으로 변환
            String safeJsonRequestBody = objectMapper.writeValueAsString(requestBodyMap);

            log.info("Gemini API 호출 시작...");
            String response = restClient.post()
                    .uri(geminiApiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(safeJsonRequestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (Exception e) {
            log.error("Gemini API 통신 실패: {}", e.getMessage(), e); // e를 추가해서 스택트레이스 확인
            throw new BusinessException(ErrorType.AI_SERVER_ERROR);
        }
    }

    private Map<String, Object> parseJsonToMap(String jsonString) {
        try {
            String cleanedJson = jsonString.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleanedJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 응답 파싱 실패. 원본 응답: {}", jsonString);
            throw new BusinessException(ErrorType.AI_RESPONSE_PARSE_ERROR);
        }
    }
}
