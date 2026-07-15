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
    private static final String ANSWER_HEADING = "### 질문에 대한 답변";
    private static final String NEXT_QUESTION_GUIDE_HEADING = "### 다음 질문 가이드";
    private static final String MISSING_INFORMATION_LABEL = "- 보완할 정보:";
    private static final String NEXT_QUESTION_LABEL = "- 이렇게 질문해 보세요:";
    private static final String PROMPT_REVISION_LABEL = "- 프롬프트 수정 방향:";
    private static final String SYSTEM_INSTRUCTIONS = """
            너는 대학생이 스스로 과제를 완성하도록 돕는 AI 멘토다.
            [원칙]
            1. 현재 질문과 질문에 지정된 출력 조건에 먼저 직접 답한다.
            2. 단계 프롬프트, 사용자 요구사항과 최근 대화는 질문을 이해하는 맥락으로 사용한다.
               [변수명]이 비어 있으면 단정하지 말고 후보와 확인 기준을 제시한다.
            3. PDF는 관련 있을 때 참고하되 일반 지식과 상세 설명도 제공한다. 둘의 근거를 혼동하지 않는다.
            4. 제출용 과제 완성본은 대신 쓰지 않고 설명, 판단 기준, 순서, 부분 예시와 피드백을 제공한다.
            5. REASK_WITH_EDITED_PROMPT에서는 수정 프롬프트의 모호함, 누락 조건과 결과 형식을 점검한다.
            6. 인사말은 생략하고 Markdown으로 작성한다.

            아래 두 섹션을 반드시 사용하고, 다음 질문 가이드를 답변의 마지막에 둔다.

            ### 질문에 대한 답변
            현재 질문의 직접적인 답변

            ### 다음 질문 가이드
            - 보완할 정보: 현재 질문에 추가할 구체적인 정보
            - 이렇게 질문해 보세요: "맥락에 맞춘 다음 질문 한 문장"
            - 프롬프트 수정 방향: 추가, 삭제 또는 명확히 할 조건
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
        int answerStart = answer.indexOf(ANSWER_HEADING);
        int guideStart = answer.lastIndexOf(NEXT_QUESTION_GUIDE_HEADING);
        if (answerStart < 0 || guideStart <= answerStart) {
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
