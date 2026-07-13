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
    private static final String SYSTEM_INSTRUCTIONS = """
            너는 대학생이 스스로 과제를 완성하도록 돕는 AI 멘토다.
            정답이나 완성본을 대신 작성하지 말고, 현재 로드맵 단계의 프롬프트와 대화 맥락을 바탕으로
            질문에 구체적으로 답하되 다음 행동을 스스로 결정할 수 있도록 안내한다.
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

    private Map<String, Object> buildRequest(AiMentorContext context, String question,
                                             AiMessageType messageType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("system_instruction", Map.of(
                "parts", List.of(Map.of("text", buildInstructions(context, messageType)))
        ));

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ConversationMessage message : context.recentMessages()) {
            contents.add(Map.of(
                    "role", message.sender() == AiMessageSender.USER ? "user" : "model",
                    "parts", List.of(Map.of("text", message.content()))
            ));
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", question))
        ));
        body.put("contents", contents);
        body.put("generationConfig", Map.of("maxOutputTokens", 1200));
        return body;
    }

    private String buildInstructions(AiMentorContext context, AiMessageType messageType) {
        String editedPrompt = context.editedPrompt() == null
                ? "수정된 프롬프트 없음"
                : context.editedPrompt();
        return SYSTEM_INSTRUCTIONS + "\n\n[최초 제공 프롬프트]\n" + context.providedPrompt()
                + "\n\n[사용자가 수정한 프롬프트]\n" + editedPrompt
                + "\n\n[요청 유형]\n" + messageType.name();
    }

    private AiMentorReply parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String answer = getAnswer(root);
            if (answer.isBlank()) {
                throw new BusinessException(ErrorType.AI_MENTOR_RESPONSE_PARSE_ERROR);
            }

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

    private Integer nullableInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.intValue() : null;
    }
}
