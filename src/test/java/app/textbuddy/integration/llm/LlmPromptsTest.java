package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorRule;
import app.textbuddy.advisor.AdvisorRuleCheck;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPromptsTest {

    private final LlmPrompts prompts = new LlmPrompts(new PromptCatalog(new DefaultResourceLoader()), new ObjectMapper());

    @Test
    void plainLanguageIncludesTheRulesAndPreservesLiteralText() {
        String text = "  Der Wert ${value} lautet {{rules}}.  ";
        PromptMessages prompt = prompts.plainLanguage(text, "de-CH");

        assertThat(prompt.systemPrompt()).contains("Du bist ein hilfreicher Assistent", "de-CH");
        assertThat(prompt.userPrompt()).contains(text, "Du bekommst einen schwer verständlichen Text", "ALLE Informationen",
                "Schreibe wichtiges zuerst", "Löse Nebensätze nach folgenden Regeln auf",
                "Eine Textzeile enthält inklusiv Leerzeichen maximal 85 Zeichen.");
    }

    @Test
    void advisorIncludesExactTextAndSelectedRulesWithoutSourceMetadata() {
        String text = "  Bitte downloaden Sie das Formular {{rules_json}}.  ";
        AdvisorRule rule = new AdvisorRule("rule-1", "Regel 1", 3, "Keine Anglizismen",
                "Vermeiden Sie downloaden.", "Nutzen Sie herunterladen.", List.of("downloaden"));
        var document = new AdvisorDocument(1, "doc-a", "Dokument A", "Kurzfassung", "Quelle", List.of(rule));
        PromptMessages prompt = prompts.advisor(text, List.of(new AdvisorRuleCheck(document, rule)));

        assertThat(prompt.systemPrompt()).contains("JSON array", "message and replacement", "may also be null");
        assertThat(prompt.userPrompt())
                .contains(text, "Copy matchedText exactly", "excerpt exactly", "necessary whitespace",
                        "\"ruleId\":\"rule-1\"", "\"documentName\":\"doc-a\"", "\"matchTerms\":[\"downloaden\"]",
                        "Keine Anglizismen", "Vermeiden Sie downloaden.", "Nutzen Sie herunterladen.")
                .doesNotContain("Dokument A", "\"page\"", "Regel 1", "/api/advisor/doc");
    }
}
