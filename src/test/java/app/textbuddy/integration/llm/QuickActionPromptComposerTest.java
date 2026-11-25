package app.textbuddy.integration.llm;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class QuickActionPromptComposerTest {

    private final QuickActionPromptComposer promptComposer =
            new QuickActionPromptComposer(new ClasspathPromptCatalog(new DefaultResourceLoader()));

    @Test
    void plainLanguageUsesOriginalSystemAndOpenAiTemplate() {
        PromptMessages promptMessages = promptComposer.plainLanguage(
                "Der Text ist kompliziert.",
                "de-CH"
        );

        assertThat(promptMessages.systemPrompt()).contains("Du bist ein hilfreicher Assistent");
        assertThat(promptMessages.userPrompt()).contains("Du bekommst einen schwer verständlichen Text");
        assertThat(promptMessages.userPrompt()).contains("Der Text ist kompliziert.");
        assertThat(promptMessages.userPrompt()).contains("ALLE Informationen");
    }
}
