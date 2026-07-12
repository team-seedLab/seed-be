package com.example.seedbe.domain.prompt.component;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptVariableResolver {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[([A-Za-z0-9_]+)]");

    public String resolve(String promptTemplate, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return promptTemplate;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(promptTemplate);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            if (!context.containsKey(key)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            Object value = context.get(key);
            String replacement = value == null ? "" : String.valueOf(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
