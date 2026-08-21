package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.CharacterSpeechPrompt;
import app.textbuddy.quickaction.FormalityPrompt;
import app.textbuddy.quickaction.MediumCurrentUser;
import app.textbuddy.quickaction.MediumPrompt;
import app.textbuddy.quickaction.QuickActionRequest;
import app.textbuddy.quickaction.QuickActionType;
import app.textbuddy.quickaction.SocialMediaPrompt;
import app.textbuddy.quickaction.SummarizePrompt;
import com.fasterxml.jackson.databind.JsonNode;

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
    public String rewrite(QuickActionType action, QuickActionRequest request, MediumCurrentUser currentUser) {
        PromptMessages prompts = switch (action) {
            case PLAIN_LANGUAGE -> quickActionPrompts.plainLanguage(request.text(), request.language());
            case BULLET_POINTS -> quickActionPrompts.bulletPoints(request.text(), request.language());
            case PROOFREAD -> quickActionPrompts.proofread(request.text(), request.language());
            case SUMMARIZE -> quickActionPrompts.summarize(
                    request.text(),
                    request.language(),
                    SummarizePrompt.fromOption(request.option()).orElseThrow()
            );
            case FORMALITY -> quickActionPrompts.formality(
                    request.text(),
                    request.language(),
                    FormalityPrompt.fromOption(request.option()).orElseThrow()
            );
            case SOCIAL_MEDIA -> quickActionPrompts.socialMedia(
                    request.text(),
                    request.language(),
                    SocialMediaPrompt.fromOption(request.option()).orElseThrow()
            );
            case MEDIUM -> quickActionPrompts.medium(
                    request.text(),
                    request.language(),
                    MediumPrompt.fromOption(request.option()).orElseThrow(),
                    currentUser
            );
            case CHARACTER_SPEECH -> quickActionPrompts.characterSpeech(
                    request.text(),
                    request.language(),
                    CharacterSpeechPrompt.fromOption(request.option()).orElseThrow()
            );
            case CUSTOM -> quickActionPrompts.custom(request.text(), request.language(), request.prompt());
        };

        return chatClient.completeText(prompts.systemPrompt(), prompts.userPrompt());
    }

    @Override
    public List<String> rewriteSentence(String sentence, String context) {
        PromptMessages prompts = structuredPrompts.sentenceRewrite(sentence, context);
        JsonNode root = chatClient.completeJson(prompts.systemPrompt(), prompts.userPrompt());
        return readStringArray(root.path("options"), root.path("alternatives"));
    }

    @Override
    public List<String> suggestSynonyms(String word, String context) {
        PromptMessages prompts = structuredPrompts.wordSynonym(word, context);
        JsonNode root = chatClient.completeJson(prompts.systemPrompt(), prompts.userPrompt());
        return root.isArray() ? readStringArray(root) : readStringArray(root.path("synonyms"));
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
