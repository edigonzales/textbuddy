package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.SocialMediaLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SocialMediaQuickActionServiceTest {

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void mapsEachSupportedOptionToItsPrompt(String option, SocialMediaPrompt expectedPrompt) {
        AtomicReference<SocialMediaPrompt> capturedPrompt = new AtomicReference<>();
        SocialMediaLlmClient socialMediaLlmClient = (text, language, prompt) -> {
            capturedPrompt.set(prompt);
            return List.of(prompt.instruction());
        };
        SocialMediaQuickActionService service = new SocialMediaQuickActionService(socialMediaLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Produktstart ist live. Team ist bereit.", "de-DE", option, null),
                handler
        );

        assertThat(capturedPrompt.get()).isEqualTo(expectedPrompt);
        assertThat(handler.completedText).isEqualTo(expectedPrompt.instruction());
        assertThat(handler.errorMessage).isNull();
    }

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void resolvesEachSupportedOption(String option, SocialMediaPrompt expectedPrompt) {
        assertThat(SocialMediaPrompt.fromOption("  " + option.toUpperCase() + "  "))
                .isEqualTo(Optional.of(expectedPrompt));
    }

    @Test
    void skipsBlankSocialMediaRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        SocialMediaLlmClient socialMediaLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        SocialMediaQuickActionService service = new SocialMediaQuickActionService(socialMediaLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new QuickActionStreamRequest("   ", "de-DE", "bluesky", null), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAMissingOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        SocialMediaLlmClient socialMediaLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        SocialMediaQuickActionService service = new SocialMediaQuickActionService(socialMediaLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Produktstart ist live. Team ist bereit.", "de-DE", null, null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Social-Media-Option ist erforderlich.");
    }

    @Test
    void reportsAnInvalidOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        SocialMediaLlmClient socialMediaLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        SocialMediaQuickActionService service = new SocialMediaQuickActionService(socialMediaLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Produktstart ist live. Team ist bereit.", "de-DE", "tiktok", null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Social-Media-Option ist ungueltig.");
    }

    @Test
    void reportsASocialMediaSpecificErrorWhenStreamingFails() {
        SocialMediaLlmClient socialMediaLlmClient = (text, language, prompt) -> {
            throw new IllegalStateException("boom");
        };
        SocialMediaQuickActionService service = new SocialMediaQuickActionService(socialMediaLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Produktstart ist live. Team ist bereit.", "de-DE", "linkedin", null),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Social-Media-Text konnte nicht erstellt werden.");
    }

    private static Stream<Object[]> supportedPrompts() {
        return Stream.of(
                new Object[]{"bluesky", SocialMediaPrompt.BLUESKY},
                new Object[]{"instagram", SocialMediaPrompt.INSTAGRAM},
                new Object[]{"linkedin", SocialMediaPrompt.LINKEDIN}
        );
    }

    private static final class RecordingQuickActionStreamHandler implements QuickActionStreamHandler {

        private final List<String> chunks = new ArrayList<>();
        private String completedText;
        private String errorMessage;

        @Override
        public void chunk(String text) {
            chunks.add(text);
        }

        @Override
        public void complete(String text) {
            completedText = text;
        }

        @Override
        public void error(String message) {
            errorMessage = message;
        }
    }
}
