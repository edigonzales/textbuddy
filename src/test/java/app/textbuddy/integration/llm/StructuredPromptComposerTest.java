package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredPromptComposerTest {

    private final StructuredPromptComposer promptComposer = new StructuredPromptComposer(
            new ClasspathPromptCatalog(new DefaultResourceLoader()),
            new ObjectMapper()
    );

    @Test
    void sentenceRewriteIncludesSentenceAndContext() {
        PromptMessages promptMessages = promptComposer.sentenceRewrite(
                "Dieser Satz ist holprig.",
                "Der Absatz gibt weitere Hintergründe."
        );

        assertThat(promptMessages.systemPrompt()).contains("valid JSON only");
        assertThat(promptMessages.userPrompt()).contains("Dieser Satz ist holprig.");
        assertThat(promptMessages.userPrompt()).contains("Der Absatz gibt weitere Hintergründe.");
    }

    @Test
    void advisorIncludesOriginalRuleInstructionAndSerializedRules() {
        PromptMessages promptMessages = promptComposer.advisor(
                "Bitte downloaden Sie das Formular.",
                List.of(new AdvisorRuleCheck(
                        "doc-a",
                        "Dokument A",
                        "/api/advisor/doc/doc-a#page=3",
                        "rule-1",
                        "Regel 1",
                        3,
                        "Keine Anglizismen",
                        "Vermeiden Sie downloaden.",
                        "Nutzen Sie herunterladen.",
                        List.of("downloaden")
                ))
        );

        assertThat(promptMessages.systemPrompt()).contains("JSON array");
        assertThat(promptMessages.userPrompt()).contains("Review only the given rules");
        assertThat(promptMessages.userPrompt()).contains("\"ruleId\":\"rule-1\"");
    }
}
