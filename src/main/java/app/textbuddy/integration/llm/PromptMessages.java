package app.textbuddy.integration.llm;

public record PromptMessages(
        String systemPrompt,
        String userPrompt
) {
}
