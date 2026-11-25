package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
import app.textbuddy.advisor.AdvisorRuleCheck;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StructuredPromptComposer {

    private static final String ADVISOR_JSON_SHELL = "shells/structured/advisor-json.system.txt";
    private static final String ADVISOR_FIX_SHELL = "shells/structured/advisor-fix.system.txt";

    private final PromptCatalog promptCatalog;
    private final ObjectMapper objectMapper;

    public StructuredPromptComposer(PromptCatalog promptCatalog, ObjectMapper objectMapper) {
        this.promptCatalog = Objects.requireNonNull(promptCatalog);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public PromptMessages advisor(String text, List<AdvisorRuleCheck> ruleChecks) {
        return new PromptMessages(
                promptCatalog.get(ADVISOR_JSON_SHELL),
                promptCatalog.render("source/structured/advisor.user.txt", Map.of(
                        "text", literal(text),
                        "rules_json", toJson(ruleChecks)
                ))
        );
    }

    public PromptMessages advisorFix(String text, List<AdvisorFixFinding> findings) {
        return new PromptMessages(
                promptCatalog.get(ADVISOR_FIX_SHELL),
                promptCatalog.render("source/structured/advisor-fix.user.txt", Map.of(
                        "text", literal(text),
                        "findings_json", toJson(findings)
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

    private String literal(String value) {
        return value == null ? "" : value;
    }
}
