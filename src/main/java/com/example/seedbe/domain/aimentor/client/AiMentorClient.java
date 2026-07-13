package com.example.seedbe.domain.aimentor.client;

import com.example.seedbe.domain.aimentor.enums.AiMessageSender;
import com.example.seedbe.domain.aimentor.enums.AiMessageType;

import java.util.List;

public interface AiMentorClient {
    AiMentorReply ask(AiMentorContext context, String question, AiMessageType messageType);

    record AiMentorContext(
            String providedPrompt,
            String editedPrompt,
            List<ConversationMessage> recentMessages
    ) {
    }

    record ConversationMessage(AiMessageSender sender, String content) {
    }

    record AiMentorReply(
            String content,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens
    ) {
    }
}
