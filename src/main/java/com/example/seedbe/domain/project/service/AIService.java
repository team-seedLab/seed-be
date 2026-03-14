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
        String jsonSchema = switch (type) {
            case REPORT -> """
                    {
                      "audience": "결과물을 읽거나 평가할 대상 (예: 전공 교수, 학우 등)",
                      "purpose": "이 리포트의 궁극적인 목적이나 평가 기준",
                      "length": "요구되는 분량 (예: A4 3장, 2000자 등 - 없으면 '제한 없음')",
                      "topic": "리포트가 다루는 핵심 주제",
                      "document": "사용자가 업로드한 원본 텍스트의 전체 또는 핵심 요약 내용"
                    }""";
            case PAPER -> """
                    {
                      "field": "논문이 다루는 구체적인 학문/전공 분야",
                      "citation_style": "요구되는 인용 스타일 (예: APA, MLA, IEEE 등)",
                      "word_count": "목표 단어 수 또는 분량 제한",
                      "topic": "논문이 주장하고자 하는 핵심 연구 주제",
                      "document": "교정 및 검토 대상이 되는 원본 텍스트 내용"
                    }""";
            case PRESENTATION -> """
                    {
                      "audience": "발표를 듣는 청중의 수준과 특성",
                      "time_limit": "발표에 주어진 제한 시간 (예: 10분, 15분 등)",
                      "topic": "발표의 핵심 주제",
                      "tone": "발표의 톤앤매너 (예: 공식적인, 설득력 있는, 캐주얼한 등)",
                      "content": "발표 대본 및 슬라이드로 변환할 원본 텍스트 내용"
                    }""";
            case EXPERIMENT -> """
                    {
                      "field": "실험 또는 공학 프로젝트의 세부 전공 분야",
                      "problem": "해결해야 할 구체적인 문제나 실험의 핵심 목표",
                      "experiment": "진행해야 할 실험의 구체적인 절차 및 내용",
                      "results": "평가 및 분석해야 할 실험 결과 데이터 또는 예상 결과"
                    }""";
            case STUDY_SUMMARY -> """
                    {
                      "course": "해당 자료를 다루는 과목명",
                      "exam_style": "교수님의 시험 출제 스타일, 강조점, 또는 요약의 주된 포커스",
                      "length": "요약본의 요구 분량 (예: 개조식 A4 1장 등)",
                      "document": "분석하고 요약할 학습 자료 원본 텍스트"
                    }""";
            case STUDY_LEARNING -> """
                    {
                      "concept": "학습자가 우선적으로 이해해야 할 핵심 개념",
                      "target_level": "학습자의 현재 지식 수준이나 목표 이해도",
                      "concept1": "연관 지식을 연결할 첫 번째 핵심 개념",
                      "concept2": "연관 지식을 연결할 두 번째 핵심 개념",
                      "topic": "퀴즈를 생성할 범위의 핵심 주제",
                      "difficulty": "생성할 퀴즈의 난이도 (예: 상, 중, 하)"
                    }""";
        };

        return String.format("""
                너는 대학생의 과제 지시서(PDF)와 추가 요구사항을 분석하여, AI 템플릿 엔진에 주입할 '핵심 변수'들을 추출하는 최고 수준의 데이터 파싱 전문가야.
                아래 [과제 텍스트]와 [유저 요구사항]을 분석하여, '%s' 과제 수행의 전 과정에 필요한 필수 파라미터를 모두 추출해 줘.
                
                [과제 텍스트]
                %s
                
                [유저 요구사항]
                %s
                
                [매우 중요한 행동 원칙]
                1. 너는 과제를 직접 수행하는 것이 아니라, AI 템플릿에 들어갈 '변수 데이터'만 추출하는 역할이야.
                2. 주어진 텍스트에서 명확히 알 수 없는 정보는 빈칸으로 두지 말고, 문맥을 바탕으로 가장 적절한 값을 추론해서 채워 넣어. (예: length가 없으면 '제한 없음', audience가 없으면 '해당 전공 교수' 등)
                3. 반드시 아래 제시된 JSON 스키마 규격에 포함된 모든 Key를 단 하나도 빠짐없이 포함하여, 순수 JSON 포맷으로만 응답해. 마크다운(```json)이나 불필요한 텍스트는 절대 쓰지 마.
                
                [요구되는 JSON 스키마]
                %s
                """,
                type.getDescription(),
                pdfText,
                (userIntent != null && !userIntent.isBlank()) ? userIntent : "특별한 요구사항 없음",
                jsonSchema
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
            JsonNode textNode = rootNode.at("/candidates/0/content/parts/0/text");
            if (textNode.isMissingNode()) {
                log.error("AI 응답에서 예상 경로의 텍스트를 찾을 수 없습니다. 원본 응답: {}", response);
                throw new BusinessException(ErrorType.AI_RESPONSE_PARSE_ERROR);
            }

            return textNode.asText();

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
