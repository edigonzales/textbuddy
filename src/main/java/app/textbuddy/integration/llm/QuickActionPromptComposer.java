package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.SummarizePrompt;

import java.util.Map;
import java.util.Objects;

public final class QuickActionPromptComposer {

    private static final String COMMON_TEXT_USER = "quick-actions/common-text.user.txt";
    private static final String LANGUAGE_AND_OUTPUT_SHELL = "shells/quick-actions/language-and-output.system.txt";
    private static final String PLAIN_LANGUAGE_SYSTEM = "source/quick-actions/plain-language/system.txt";
    private static final String PLAIN_LANGUAGE_OPENAI_TEMPLATE = "source/quick-actions/plain-language/openai-template.user.txt";
    private static final String PLAIN_LANGUAGE_REWRITE_COMPLETE = "source/quick-actions/plain-language/rewrite-complete.txt";
    private static final String PLAIN_LANGUAGE_RULES = "source/quick-actions/plain-language/rules-ls.txt";

    private final PromptCatalog promptCatalog;

    public QuickActionPromptComposer(PromptCatalog promptCatalog) {
        this.promptCatalog = Objects.requireNonNull(promptCatalog);
    }

    public PromptMessages plainLanguage(String text, String language) {
        return new PromptMessages(
                joinPrompts(
                        promptCatalog.get(PLAIN_LANGUAGE_SYSTEM),
                        renderLanguageAndOutputShell(language)
                ),
                promptCatalog.render(PLAIN_LANGUAGE_OPENAI_TEMPLATE, Map.of(
                        "completeness", promptCatalog.get(PLAIN_LANGUAGE_REWRITE_COMPLETE),
                        "rules", promptCatalog.get(PLAIN_LANGUAGE_RULES),
                        "text", valueOrEmpty(text)
                ))
        );
    }

    public PromptMessages summarize(String text, String language, SummarizePrompt prompt) {
        return quickAction("source/quick-actions/summarize.system.txt", text, language, Map.of(
                "summary_requirement", summarizeRequirement(prompt)
        ));
    }

    private PromptMessages quickAction(String sourceKey, String text, String language, Map<String, ?> variables) {
        return new PromptMessages(
                joinPrompts(
                        promptCatalog.render(sourceKey, variables),
                        renderLanguageAndOutputShell(language)
                ),
                promptCatalog.render(COMMON_TEXT_USER, Map.of("text", valueOrEmpty(text)))
        );
    }

    private String renderLanguageAndOutputShell(String language) {
        return promptCatalog.render(LANGUAGE_AND_OUTPUT_SHELL, Map.of(
                "language_instruction", languageInstruction(language)
        ));
    }

    private String languageInstruction(String language) {
        String normalized = normalize(language);

        if (normalized.isBlank() || normalized.equalsIgnoreCase("auto")) {
            return """
                    Antworte in derselben Sprache wie der Eingabetext.
                    Wenn der Eingabetext Deutsch ist, verwende die in der Schweiz übliche Rechtschreibung mit «ss» statt «ß».
                    """.strip();
        }

        return """
                Antworte in derselben Sprache wie der Eingabetext und orientiere dich an der Locale {{language}}.
                Wenn die Ausgabe Deutsch ist, verwende die in der Schweiz übliche Rechtschreibung mit «ss» statt «ß».
                """.replace("{{language}}", normalized).strip();
    }

    private String summarizeRequirement(SummarizePrompt prompt) {
        return switch (prompt) {
            case SENTENCE -> "The summary should be exactly one sentence long.";
            case THREE_SENTENCE -> "The summary should be exactly three sentences long.";
            case PARAGRAPH -> "The summary should be one paragraph long.";
            case PAGE -> "The summary should be up to one page long.";
            case MANAGEMENT_SUMMARY -> """
                    as a management summary.
                    A management summary is a summary of the key points of a text for the management team.
                    A management summary's length should be one paragraph up to one page long, depending on the length of the text.
                    """.strip();
        };
    }

    private String joinPrompts(String... parts) {
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            String normalized = normalize(part);

            if (normalized.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }

            builder.append(normalized);
        }

        return builder.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
