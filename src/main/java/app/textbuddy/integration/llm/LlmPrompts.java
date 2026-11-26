package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.SummarizePrompt;

import app.textbuddy.advisor.AdvisorFixFinding;
import app.textbuddy.advisor.AdvisorRuleCheck;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LlmPrompts {

    private static final String COMMON_TEXT_USER = "quick-actions/common-text.user.txt";
    private static final String LANGUAGE_AND_OUTPUT_SHELL = "shells/quick-actions/language-and-output.system.txt";
    private static final String PLAIN_LANGUAGE_SYSTEM = "source/quick-actions/plain-language/system.txt";
    private static final String PLAIN_LANGUAGE_OPENAI_TEMPLATE = "source/quick-actions/plain-language/openai-template.user.txt";
    private static final String PLAIN_LANGUAGE_REWRITE_COMPLETE = "source/quick-actions/plain-language/rewrite-complete.txt";
    private static final String PLAIN_LANGUAGE_RULES = "source/quick-actions/plain-language/rules-ls.txt";

    private final PromptCatalog promptCatalog;
    private final ObjectMapper objectMapper;

    public LlmPrompts(PromptCatalog promptCatalog, ObjectMapper objectMapper) {
        this.promptCatalog = promptCatalog;
        this.objectMapper = objectMapper;
    }

    public PromptMessages plainLanguage(String text, String language) {
        return new PromptMessages(
                withLanguage(promptCatalog.get(PLAIN_LANGUAGE_SYSTEM), language),
                promptCatalog.render(PLAIN_LANGUAGE_OPENAI_TEMPLATE, Map.of(
                        "completeness", promptCatalog.get(PLAIN_LANGUAGE_REWRITE_COMPLETE),
                        "rules", promptCatalog.get(PLAIN_LANGUAGE_RULES),
                        "text", text
                ))
        );
    }

    public PromptMessages summarize(String text, String language, SummarizePrompt prompt) {
        String instructions = promptCatalog.render("source/quick-actions/summarize.system.txt", Map.of(
                "summary_requirement", prompt.requirement()
        ));
        return new PromptMessages(
                withLanguage(instructions, language),
                promptCatalog.render(COMMON_TEXT_USER, Map.of("text", text))
        );
    }

    public PromptMessages advisor(String text, List<AdvisorRuleCheck> rules) {
        return new PromptMessages(
                promptCatalog.get("shells/structured/advisor-json.system.txt"),
                promptCatalog.render("source/structured/advisor.user.txt", Map.of(
                        "text", text,
                        "rules_json", objectMapper.writeValueAsString(rules)
                ))
        );
    }

    public PromptMessages advisorFix(List<AdvisorFixFinding> findings) {
        return new PromptMessages(
                promptCatalog.get("shells/structured/advisor-fix.system.txt"),
                promptCatalog.render("source/structured/advisor-fix.user.txt", Map.of(
                        "findings_json", objectMapper.writeValueAsString(findings)
                ))
        );
    }

    private String withLanguage(String instructions, String language) {
        return instructions.strip() + "\n\n" + promptCatalog.render(LANGUAGE_AND_OUTPUT_SHELL, Map.of(
                "language_instruction", languageInstruction(language)
        ));
    }

    private String languageInstruction(String language) {
        String normalized = Objects.requireNonNullElse(language, "").trim();

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
}
