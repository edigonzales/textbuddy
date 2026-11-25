package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
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
        JsonNode root = chatClient.completeJson(prompt.systemPrompt(), prompt.userPrompt());
        JsonNode matchesNode = root.isArray() ? root : root.path("matches");

        if (!matchesNode.isArray()) {
            throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Befunde.");
        }

        List<AdvisorRuleMatch> matches = new ArrayList<>();

        for (JsonNode node : matchesNode) {
            if (text(node, "documentName").isBlank() || text(node, "ruleId").isBlank()
                    || !node.path("matchedText").isString() || rawText(node, "matchedText").isBlank()) {
                throw new LlmProviderException("LLM-Provider lieferte unvollständige Advisor-Befunde.");
            }
            matches.add(new AdvisorRuleMatch(
                    text(node, "documentName"),
                    text(node, "ruleId"),
                    rawText(node, "matchedText"),
                    rawText(node, "excerpt"),
                    text(node, "message"),
                    text(node, "suggestion")
            ));
        }

        return List.copyOf(matches);
    }

    @Override
    public List<String> fixAdvisor(List<AdvisorFixFinding> findings) {
        PromptMessages prompt = prompts.advisorFix(findings);
        JsonNode root = chatClient.completeJson(prompt.systemPrompt(), prompt.userPrompt());
        if (!root.isArray() || root.size() != findings.size()) {
            throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Korrekturen.");
        }
        List<String> replacements = new ArrayList<>(root.size());
        for (JsonNode item : root) {
            if (!item.isString()) {
                throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Korrekturen.");
            }
            replacements.add(item.asString());
        }
        return replacements;
    }

    private String text(JsonNode node, String field) {
        return normalize(node.path(field).asString(""));
    }

    private String rawText(JsonNode node, String field) {
        return node.path(field).asString("");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
