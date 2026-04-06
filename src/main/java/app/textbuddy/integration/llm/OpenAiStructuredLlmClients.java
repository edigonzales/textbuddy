package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OpenAiStructuredLlmClients implements
        LlmClientFacade,
        WordSynonymLlmClient,
        AdvisorValidationLlmClient {

    private final OpenAiCompatibleChatClient chatClient;
    private final StructuredPromptComposer promptComposer;

    public OpenAiStructuredLlmClients(
            OpenAiCompatibleChatClient chatClient,
            StructuredPromptComposer promptComposer
    ) {
        this.chatClient = Objects.requireNonNull(chatClient);
        this.promptComposer = Objects.requireNonNull(promptComposer);
    }

    @Override
    public List<String> rewriteSentence(String sentence, String context) {
        PromptMessages promptMessages = promptComposer.sentenceRewrite(sentence, context);
        JsonNode root = chatClient.completeJson(promptMessages.systemPrompt(), promptMessages.userPrompt());

        return readStringArray(root.path("options"), root.path("alternatives"));
    }

    @Override
    public List<String> suggestSynonyms(String word, String context) {
        PromptMessages promptMessages = promptComposer.wordSynonym(word, context);
        JsonNode root = chatClient.completeJson(promptMessages.systemPrompt(), promptMessages.userPrompt());

        if (root.isArray()) {
            return readStringArray(root);
        }

        return readStringArray(root.path("synonyms"));
    }

    @Override
    public List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks) {
        PromptMessages promptMessages = promptComposer.advisor(text, ruleChecks);
        JsonNode root = chatClient.completeJson(promptMessages.systemPrompt(), promptMessages.userPrompt());

        JsonNode matchesNode = root.isArray() ? root : root.path("matches");

        if (!matchesNode.isArray()) {
            return List.of();
        }

        List<AdvisorRuleMatch> matches = new ArrayList<>();

        for (JsonNode node : matchesNode) {
            matches.add(new AdvisorRuleMatch(
                    text(node, "documentName"),
                    text(node, "ruleId"),
                    text(node, "matchedText"),
                    text(node, "excerpt"),
                    text(node, "message"),
                    text(node, "suggestion")
            ));
        }

        return List.copyOf(matches);
    }

    private List<String> readStringArray(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && candidate.isArray()) {
                List<String> values = new ArrayList<>();

                for (JsonNode item : candidate) {
                    if (item != null && item.isTextual()) {
                        String value = normalize(item.asText(""));

                        if (!value.isBlank()) {
                            values.add(value);
                        }
                    }
                }

                return List.copyOf(values);
            }
        }

        return List.of();
    }

    private String text(JsonNode node, String field) {
        return normalize(node.path(field).asText(""));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
