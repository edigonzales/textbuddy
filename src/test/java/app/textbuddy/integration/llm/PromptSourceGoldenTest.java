package app.textbuddy.integration.llm;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class PromptSourceGoldenTest {

    private final PromptCatalog promptCatalog = new ClasspathPromptCatalog(new DefaultResourceLoader());

    @Test
    void plainLanguageRuleBlockContainsOriginalLeichteSprachePassages() {
        String prompt = promptCatalog.get("source/quick-actions/plain-language/rules-ls.txt");

        assertThat(prompt).contains("Schreibe wichtiges zuerst");
        assertThat(prompt).contains("Löse Nebensätze nach folgenden Regeln auf");
        assertThat(prompt).contains("Eine Textzeile enthält inklusiv Leerzeichen maximal 85 Zeichen.");
    }

    @Test
    void mediumEmailPromptContainsOriginalGuidelineMarkers() {
        String prompt = promptCatalog.get("source/quick-actions/medium-email.system.txt");

        assertThat(prompt).contains("KISS-Prinzip");
        assertThat(prompt).contains("Betreff: [Optimierter Betreff]");
    }

    @Test
    void officialLetterPromptContainsOriginalCitizenCentricPrinciples() {
        String prompt = promptCatalog.get("source/quick-actions/medium-official-letter.system.txt");

        assertThat(prompt).contains("Persönlich, Sachgerecht, Verständlich");
        assertThat(prompt).contains("Brückenschlag");
    }

    @Test
    void structuredPromptsContainOriginalInstructionFragments() {
        assertThat(promptCatalog.get("source/structured/sentence-rewrite.user.txt"))
                .contains("Generate at least 1 but maximum of 5 alternative rewrites");
        assertThat(promptCatalog.get("source/structured/advisor.user.txt"))
                .contains("Review only the given rules");
    }
}
