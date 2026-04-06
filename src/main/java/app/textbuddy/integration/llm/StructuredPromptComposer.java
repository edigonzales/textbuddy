package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StructuredPromptComposer {

    private static final String SENTENCE_REWRITE_JSON_SHELL = "shells/structured/sentence-rewrite-json.system.txt";
    private static final String WORD_SYNONYM_JSON_SHELL = "shells/structured/word-synonym-json.system.txt";
    private static final String ADVISOR_JSON_SHELL = "shells/structured/advisor-json.system.txt";
    private static final String NO_CONTEXT_MESSAGE = "Es liegt kein zusätzlicher Kontext vor.";

    private final PromptCatalog promptCatalog;
    private final ObjectMapper objectMapper;

    public StructuredPromptComposer(PromptCatalog promptCatalog, ObjectMapper objectMapper) {
        this.promptCatalog = Objects.requireNonNull(promptCatalog);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public PromptMessages sentenceRewrite(String sentence, String context) {
        return new PromptMessages(
                promptCatalog.get(SENTENCE_REWRITE_JSON_SHELL),
                promptCatalog.render("source/structured/sentence-rewrite.user.txt", Map.of(
                        "sentence", normalize(sentence),
                        "context", defaultContext(context)
                ))
        );
    }

    public PromptMessages wordSynonym(String word, String context) {
        return new PromptMessages(
                promptCatalog.get(WORD_SYNONYM_JSON_SHELL),
                promptCatalog.render("source/structured/word-synonym.user.txt", Map.of(
                        "word", normalize(word),
                        "context", defaultContext(context)
                ))
        );
    }

    public PromptMessages advisor(String text, List<AdvisorRuleCheck> ruleChecks) {
        return new PromptMessages(
                promptCatalog.get(ADVISOR_JSON_SHELL),
                promptCatalog.render("source/structured/advisor.user.txt", Map.of(
                        "text", normalize(text),
                        "rules_json", toJson(ruleChecks)
                ))
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Prompt-Regeln konnten nicht serialisiert werden.", exception);
        }
    }

    private String defaultContext(String context) {
        String normalized = normalize(context);
        return normalized.isBlank() ? NO_CONTEXT_MESSAGE : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
