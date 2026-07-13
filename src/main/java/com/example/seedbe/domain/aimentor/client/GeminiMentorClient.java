package com.example.seedbe.domain.aimentor.client;

import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiMentorClient implements AiMentorClient {
    private static final String NEXT_QUESTION_GUIDE_HEADING = "### 다음 질문 가이드";
    private static final String MISSING_INFORMATION_LABEL = "- 보완할 정보:";
    private static final String NEXT_QUESTION_LABEL = "- 이렇게 질문해 보세요:";
    private static final String PROMPT_REVISION_LABEL = "- 프롬프트 수정 방향:";
    private static final String SYSTEM_INSTRUCTIONS = """
            너는 대학생이 스스로 과제를 완성하도록 돕는 AI 멘토다.
            정답이나 완성본을 대신 작성하지 말고, 현재 로드맵 단계와 PDF 근거, 사용자 요구사항,
            대화 맥락을 바탕으로 학습자가 스스로 판단하고 다음 작업을 수행할 수 있도록 안내한다.

            [답변 원칙]
            1. 인사말이나 막연한 격려보다 사용자의 질문에 대한 핵심 답변부터 제시한다.
            2. PDF에서 검색한 관련 내용이 있으면 그 내용을 근거로 설명하되, 없는 사실을 만들지 않는다.
            3. 과제의 완성본을 대신 작성하기보다 판단 기준, 접근 순서, 예시와 확인 질문을 제공한다.
            4. 사용자의 질문이 모호하면 무엇이 부족한지 구체적으로 알려 준다.
            5. REASK_WITH_EDITED_PROMPT 요청에서는 수정된 프롬프트의 모호한 표현, 누락된 조건,
               결과 형식을 우선 점검하고 더 명확한 수정 방향을 제시한다.
            6. 답변은 읽기 쉬운 Markdown으로 작성한다.

            [필수 종료 형식]
            모든 답변은 설명을 마친 뒤 반드시 아래 섹션을 마지막에 포함하고, 이 섹션 뒤에는 다른 내용을 쓰지 않는다.

            ### 다음 질문 가이드
            - 보완할 정보: 현재 질문에서 추가하면 학습에 도움이 되는 구체적인 정보
            - 이렇게 질문해 보세요: "현재 단계와 맥락에 맞게 구체화한 다음 질문 한 문장"
            - 프롬프트 수정 방향: 현재 프롬프트에서 추가, 삭제 또는 명확히 할 조건

            각 항목은 현재 질문과 과제 맥락에 맞게 구체적으로 작성해야 하며, 형식만 반복하거나 일반론으로 채우지 않는다.
            """;

    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiMentorClient(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public AiMentorReply ask(AiMentorContext context, String question, AiMessageType messageType) {
        try {
            String response = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(context, question, messageType))
                    .retrieve()
                    .body(String.class);
            return parseResponse(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini AI mentor call failed: {}", e.getMessage());
            throw new BusinessException(ErrorType.AI_MENTOR_SERVER_ERROR);
        }
    }

    Map<String, Object> buildRequest(AiMentorContext context, String question,
                                     AiMessageType messageType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("system_instruction", Map.of(
                "parts", List.of(Map.of("text", buildInstructions(context, messageType)))
        ));

        List<Map<String, Object>> contents = normalizeHistory(context.recentMessages());
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", question))
        ));
        body.put("contents", contents);
        body.put("generationConfig", Map.of("maxOutputTokens", 4096));
        return body;
    }

    private List<Map<String, Object>> normalizeHistory(List<ConversationMessage> messages) {
        List<Map<String, Object>> contents = new ArrayList<>();
        ConversationMessage pendingUser = null;

        for (ConversationMessage message : messages) {
            if (message.sender() == AiMessageSender.USER) {
                pendingUser = message;
                continue;
            }
            if (message.sender() == AiMessageSender.ASSISTANT && pendingUser != null) {
                contents.add(content("user", pendingUser.content()));
                contents.add(content("model", message.content()));
                pendingUser = null;
            }
        }
        return contents;
    }

    private Map<String, Object> content(String role, String text) {
        return Map.of(
                "role", role,
                "parts", List.of(Map.of("text", text))
        );
    }

    private String buildInstructions(AiMentorContext context, AiMessageType messageType) {
        return SYSTEM_INSTRUCTIONS
                + "\n\n[사용자가 원하는 결과]\n" + valueOrEmpty(context.desiredOutcome())
                + "\n\n[핵심 관점]\n" + valueOrEmpty(context.keyFocus())
                + "\n\n[필수 포함 요소]\n" + valueOrEmpty(context.requiredElements())
                + "\n\n[현재 단계의 최종 프롬프트]\n" + context.finalPrompt()
                + "\n\n[PDF에서 검색한 관련 내용]\n" + context.relevantSourceContext()
                + "\n\n[요청 유형]\n" + messageType.name();
    }

    private String valueOrEmpty(String value) {
        return value == null || value.isBlank() ? "입력 없음" : value;
    }

    AiMentorReply parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String finishReason = root.at("/candidates/0/finishReason").asText();
            if ("MAX_TOKENS".equals(finishReason)) {
                throw new BusinessException(ErrorType.AI_MENTOR_RESPONSE_TRUNCATED);
            }
            String answer = getAnswer(root);
            if (answer.isBlank()) {
                throw new BusinessException(ErrorType.AI_MENTOR_RESPONSE_PARSE_ERROR);
            }
            validateMentorFormat(answer);

            JsonNode usage = root.path("usageMetadata");
            return new AiMentorReply(
                    answer,
                    nullableInt(usage, "promptTokenCount"),
                    nullableInt(usage, "candidatesTokenCount"),
                    nullableInt(usage, "totalTokenCount")
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini AI mentor response parse failed: {}", e.getMessage());
            throw new BusinessException(ErrorType.AI_MENTOR_RESPONSE_PARSE_ERROR);
        }
    }

    private String getAnswer(JsonNode root) {
        StringBuilder answer = new StringBuilder();
        for (JsonNode part : root.at("/candidates/0/content/parts")) {
            JsonNode text = part.path("text");
            if (text.isTextual()) {
                answer.append(text.asText());
            }
        }
        return answer.toString();
    }

    private void validateMentorFormat(String answer) {
        int guideStart = answer.lastIndexOf(NEXT_QUESTION_GUIDE_HEADING);
        if (guideStart < 0) {
            throw new BusinessException(ErrorType.AI_MENTOR_RESPONSE_FORMAT_ERROR);
        }
        String guide = answer.substring(guideStart);
        if (!guide.contains(MISSING_INFORMATION_LABEL)
                || !guide.contains(NEXT_QUESTION_LABEL)
                || !guide.contains(PROMPT_REVISION_LABEL)) {
            throw new BusinessException(ErrorType.AI_MENTOR_RESPONSE_FORMAT_ERROR);
        }
    }

    private Integer nullableInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.intValue() : null;
    }
}
