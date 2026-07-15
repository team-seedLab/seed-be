package com.example.seedbe.domain.aimentor.client;

import com.example.seedbe.domain.aimentor.enums.AiMessageType;
import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiMentorClientTest {
    private final GeminiMentorClient client = new GeminiMentorClient("key", "https://example.com");

    @Test
    @SuppressWarnings("unchecked")
    void reservesEnoughTokensForThinkingAndVisibleAnswer() {
        AiMentorClient.AiMentorContext context = new AiMentorClient.AiMentorContext(
                "final prompt", "source", "outcome", "focus", "elements", List.of());

        Map<String, Object> request = client.buildRequest(context, "question", AiMessageType.CHAT);

        Map<String, Object> generationConfig = (Map<String, Object>) request.get("generationConfig");
        assertThat(generationConfig)
                .containsEntry("maxOutputTokens", 4096)
                .containsEntry("thinkingConfig", Map.of("thinkingLevel", "minimal"));
        Map<String, Object> responseFormat = (Map<String, Object>) generationConfig.get("responseFormat");
        Map<String, Object> textFormat = (Map<String, Object>) responseFormat.get("text");
        assertThat(textFormat)
                .containsEntry("mimeType", "APPLICATION_JSON")
                .containsKey("schema");
    }

    @Test
    @SuppressWarnings("unchecked")
    void requiresMentorAnswerToEndWithNextQuestionGuide() {
        AiMentorClient.AiMentorContext context = new AiMentorClient.AiMentorContext(
                "final prompt", "source", "outcome", "focus", "elements", List.of());

        Map<String, Object> request = client.buildRequest(
                context, "question", AiMessageType.REASK_WITH_EDITED_PROMPT);
        Map<String, Object> systemInstruction = (Map<String, Object>) request.get("system_instruction");
        List<Map<String, String>> parts = (List<Map<String, String>>) systemInstruction.get("parts");
        String instruction = parts.getFirst().get("text");

        assertThat(instruction)
                .contains("REASK_WITH_EDITED_PROMPT")
                .contains("현재 질문과 질문에 지정된 출력 조건에 먼저 직접 답한다")
                .contains("일반 지식과 상세 설명도 제공한다")
                .contains("제출용 과제 완성본은 대신 쓰지 않고")
                .contains("다음 질문 가이드에는 보완할 정보")
                .hasSizeLessThan(1_500);
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalizesHistoryToAlternatingUserAndModelRoles() {
        List<AiMentorClient.ConversationMessage> malformedHistory = List.of(
                new AiMentorClient.ConversationMessage(AiMessageSender.ASSISTANT, "orphan model"),
                new AiMentorClient.ConversationMessage(AiMessageSender.USER, "old user"),
                new AiMentorClient.ConversationMessage(AiMessageSender.USER, "latest user"),
                new AiMentorClient.ConversationMessage(AiMessageSender.ASSISTANT, "model answer"),
                new AiMentorClient.ConversationMessage(AiMessageSender.USER, "orphan user")
        );
        AiMentorClient.AiMentorContext context = new AiMentorClient.AiMentorContext(
                "final prompt", "source", "outcome", "focus", "elements", malformedHistory);

        Map<String, Object> request = client.buildRequest(context, "current question", AiMessageType.CHAT);
        List<Map<String, Object>> contents = (List<Map<String, Object>>) request.get("contents");

        assertThat(contents).extracting(content -> content.get("role"))
                .containsExactly("user", "model", "user");
        List<Map<String, String>> firstParts = (List<Map<String, String>>) contents.getFirst().get("parts");
        assertThat(firstParts.getFirst().get("text")).isEqualTo("latest user");
    }

    @Test
    void parsesCompletedAnswerAndTokenUsage() {
        String response = """
                {
                  "candidates": [{
                    "content": {"parts": [{"text": "{\\"answer\\":\\"핵심 답변\\",\\"missingInformation\\":\\"적용 대상\\",\\"nextQuestion\\":\\"적용 대상을 어떻게 정하나요?\\",\\"promptRevision\\":\\"대상과 범위를 명시하세요.\\"}"}]},
                    "finishReason": "STOP"
                  }],
                  "usageMetadata": {
                    "promptTokenCount": 100,
                    "candidatesTokenCount": 20,
                    "totalTokenCount": 120
                  }
                }
                """;

        AiMentorClient.AiMentorReply reply = client.parseResponse(response);

        assertThat(reply.content()).contains("### 질문에 대한 답변", "핵심 답변", "### 다음 질문 가이드");
        assertThat(reply.inputTokens()).isEqualTo(100);
        assertThat(reply.outputTokens()).isEqualTo(20);
        assertThat(reply.totalTokens()).isEqualTo(120);
    }

    @Test
    void rejectsTruncatedAnswerInsteadOfSavingPartialContent() {
        String response = """
                {
                  "candidates": [{
                    "content": {"parts": [{"text": "중간에 잘린 답변"}]},
                    "finishReason": "MAX_TOKENS"
                  }]
                }
                """;

        assertThatThrownBy(() -> client.parseResponse(response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_RESPONSE_TRUNCATED);
    }

    @Test
    void rejectsAnswerWithoutRequiredStructuredFields() {
        String response = """
                {
                  "candidates": [{
                    "content": {"parts": [{"text": "{\\"answer\\":\\"핵심 답변\\"}"}]},
                    "finishReason": "STOP"
                  }]
                }
                """;

        assertThatThrownBy(() -> client.parseResponse(response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_RESPONSE_FORMAT_ERROR);
    }

    @Test
    void rejectsBlankRequiredStructuredField() {
        String response = """
                {
                  "candidates": [{
                    "content": {"parts": [{"text": "{\\"answer\\":\\"\\",\\"missingInformation\\":\\"적용 대상\\",\\"nextQuestion\\":\\"질문\\",\\"promptRevision\\":\\"범위를 추가하세요.\\"}"}]},
                    "finishReason": "STOP"
                  }]
                }
                """;

        assertThatThrownBy(() -> client.parseResponse(response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_RESPONSE_FORMAT_ERROR);
    }
}
