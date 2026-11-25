package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.QuickActionRequest;
import app.textbuddy.quickaction.QuickActionType;
import app.textbuddy.quickaction.SummarizePrompt;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OpenAiTextbuddyLlmClient implements TextbuddyLlmClient {

    private final OpenAiCompatibleChatClient chatClient;
    private final QuickActionPromptComposer quickActionPrompts;
    private final StructuredPromptComposer structuredPrompts;

    public OpenAiTextbuddyLlmClient(
            OpenAiCompatibleChatClient chatClient,
            QuickActionPromptComposer quickActionPrompts,
            StructuredPromptComposer structuredPrompts
    ) {
        this.chatClient = Objects.requireNonNull(chatClient);
        this.quickActionPrompts = Objects.requireNonNull(quickActionPrompts);
        this.structuredPrompts = Objects.requireNonNull(structuredPrompts);
    }

    @Override
    public String rewrite(QuickActionType action, QuickActionRequest request) {
        PromptMessages prompts = switch (action) {
            case PLAIN_LANGUAGE -> quickActionPrompts.plainLanguage(request.text(), request.language());
            case SUMMARIZE -> quickActionPrompts.summarize(
                    request.text(),
                    request.language(),
                    SummarizePrompt.fromOption(request.option()).orElseThrow()
            );
        };

        return chatClient.completeText(prompts.systemPrompt(), prompts.userPrompt());
    }

    @Override
    public List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks) {
        PromptMessages prompts = structuredPrompts.advisor(text, ruleChecks);
        JsonNode root = chatClient.completeJson(prompts.systemPrompt(), prompts.userPrompt());
        JsonNode matchesNode = root.isArray() ? root : root.path("matches");

        if (!matchesNode.isArray()) {
            return List.of();
        }

        List<AdvisorRuleMatch> matches = new ArrayList<>();

        for (JsonNode node : matchesNode) {
            matches.add(new AdvisorRuleMatch(
                    text(node, "documentName"),
                    text(node, "ruleId"),
                    rawText(node, "matchedText"),
                    text(node, "excerpt"),
                    text(node, "message"),
                    text(node, "suggestion")
            ));
        }

        return List.copyOf(matches);
    }

    @Override
    public String fixAdvisor(String text, List<AdvisorFixFinding> findings) {
        PromptMessages prompts = structuredPrompts.advisorFix(text, findings);
        return chatClient.completeText(prompts.systemPrompt(), prompts.userPrompt());
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
