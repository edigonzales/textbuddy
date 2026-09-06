package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.CharacterSpeechPrompt;
import app.textbuddy.quickaction.MediumCurrentUser;
import app.textbuddy.quickaction.MediumPrompt;
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

    @Test
    void mediumEmailInjectsCurrentUserPlaceholdersOrRealValues() {
        PromptMessages promptMessages = promptComposer.medium(
                "Projekt ist freigegeben.",
                "de-CH",
                MediumPrompt.EMAIL,
                new MediumCurrentUser("Ada", "Lovelace", "ada@example.org")
        );

        assertThat(promptMessages.systemPrompt()).contains("Ada");
        assertThat(promptMessages.systemPrompt()).contains("ada@example.org");
        assertThat(promptMessages.systemPrompt()).contains("KISS-Prinzip");
    }

    @Test
    void characterSpeechUsesOriginalDirectSpeechRules() {
        PromptMessages promptMessages = promptComposer.characterSpeech(
                "Sie sagte, dass sie komme.",
                "de-CH",
                CharacterSpeechPrompt.DIRECT_SPEECH
        );

        assertThat(promptMessages.systemPrompt()).contains("German Direct Speech Rules");
        assertThat(promptMessages.systemPrompt()).contains("Möchtest du zum Fussball gehen?");
    }
}
