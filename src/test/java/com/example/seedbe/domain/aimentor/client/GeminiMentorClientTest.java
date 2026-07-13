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
    void reservesEnoughTokensForThinkingAndVisibleAnswer() {
        AiMentorClient.AiMentorContext context = new AiMentorClient.AiMentorContext(
                "final prompt", "source", "outcome", "focus", "elements", List.of());

        Map<String, Object> request = client.buildRequest(context, "question", AiMessageType.CHAT);

        assertThat(request.get("generationConfig"))
                .isEqualTo(Map.of("maxOutputTokens", 4096));
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
                .contains("### 다음 질문 가이드")
                .contains("- 보완할 정보:")
                .contains("- 이렇게 질문해 보세요:")
                .contains("- 프롬프트 수정 방향:")
                .contains("이 섹션 뒤에는 다른 내용을 쓰지 않는다");
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
                    "content": {"parts": [{"text": "핵심 답변\\n\\n### 다음 질문 가이드\\n- 보완할 정보: 적용 대상\\n- 이렇게 질문해 보세요: \\"적용 대상을 어떻게 정하나요?\\"\\n- 프롬프트 수정 방향: 대상과 범위를 명시하세요."}]},
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

        assertThat(reply.content()).contains("핵심 답변", "### 다음 질문 가이드");
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
    void rejectsAnswerWithoutRequiredNextQuestionGuide() {
        String response = """
                {
                  "candidates": [{
                    "content": {"parts": [{"text": "형식 없이 끝난 답변"}]},
                    "finishReason": "STOP"
                  }]
                }
                """;

        assertThatThrownBy(() -> client.parseResponse(response))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.AI_MENTOR_RESPONSE_FORMAT_ERROR);
    }
}
