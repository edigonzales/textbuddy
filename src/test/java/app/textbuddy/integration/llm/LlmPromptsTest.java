package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
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
    void advisorIncludesExactTextAndSelectedRules() {
        String text = "Bitte downloaden Sie das Formular {{rules_json}}.";
        PromptMessages prompt = prompts.advisor(text, List.of(new AdvisorRuleCheck(
                "doc-a", "Dokument A", "/api/advisor/doc/doc-a#page=3", "rule-1", "Regel 1", 3,
                "Keine Anglizismen", "Vermeiden Sie downloaden.", "Nutzen Sie herunterladen.", List.of("downloaden")
        )));

        assertThat(prompt.systemPrompt()).contains("JSON array");
        assertThat(prompt.userPrompt()).contains(text, "Review only the given rules", "matchedText` must be copied exactly",
                "\"ruleId\":\"rule-1\"");
    }

    @Test
    void advisorFixRequestsOnlyOrderedReplacementText() {
        PromptMessages prompt = prompts.advisorFix(List.of(
                new AdvisorFixFinding("per sofort", "  Bitte per sofort handeln.  ", "Prüfe die Formulierung.", "ab sofort")
        ));

        assertThat(prompt.systemPrompt()).contains("JSON array of strings", "one replacement per finding");
        assertThat(prompt.userPrompt()).contains("matchedText alone", "read-only context",
                "\"matchedText\":\"per sofort\"", "\"suggestion\":\"ab sofort\"", "  Bitte per sofort handeln.  ");
    }
}
