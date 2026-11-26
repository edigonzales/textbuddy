package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.SummarizePrompt;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class OpenAiTextbuddyLlmClient implements TextbuddyLlmClient {

    private final OpenAiCompatibleChatClient chatClient;
    private final LlmPrompts prompts;

    public OpenAiTextbuddyLlmClient(OpenAiCompatibleChatClient chatClient, LlmPrompts prompts) {
        this.chatClient = chatClient;
        this.prompts = prompts;
    }

    @Override
    public String rewrite(String text, String language, SummarizePrompt summary) {
        PromptMessages prompt = summary == null ? prompts.plainLanguage(text, language)
                : prompts.summarize(text, language, summary);
        return chatClient.completeText(prompt.systemPrompt(), prompt.userPrompt());
    }

    @Override
    public List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks) {
        PromptMessages prompt = prompts.advisor(text, ruleChecks);
        JsonNode matchesNode = chatClient.completeJson(prompt.systemPrompt(), prompt.userPrompt());
        if (!matchesNode.isArray()) {
            throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Befunde.");
        }

        List<AdvisorRuleMatch> matches = new ArrayList<>();
        for (JsonNode node : matchesNode) {
            JsonNode replacement = node.path("replacement");
            if (!replacement.isNull() && !replacement.isString()) {
                throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Befunde.");
            }
            matches.add(new AdvisorRuleMatch(
                    requiredText(node, "documentName"),
                    requiredText(node, "ruleId"),
                    requiredText(node, "matchedText"),
                    requiredText(node, "excerpt"),
                    requiredText(node, "message"),
                    replacement.isNull() ? null : replacement.asString()
            ));
        }
        return List.copyOf(matches);
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isString() || value.asString().isBlank()) {
            throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Befunde.");
        }
        return value.asString();
    }
}
