package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.CharacterSpeechLlmClient;
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

class CharacterSpeechQuickActionServiceTest {

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void mapsEachSupportedOptionToItsPrompt(String option, CharacterSpeechPrompt expectedPrompt) {
        AtomicReference<CharacterSpeechPrompt> capturedPrompt = new AtomicReference<>();
        CharacterSpeechLlmClient characterSpeechLlmClient = (text, language, prompt) -> {
            capturedPrompt.set(prompt);
            return List.of(prompt.instruction());
        };
        CharacterSpeechQuickActionService service = new CharacterSpeechQuickActionService(characterSpeechLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Projekt ist freigegeben. Team startet am Montag.", "de-DE", option, null),
                handler
        );

        assertThat(capturedPrompt.get()).isEqualTo(expectedPrompt);
        assertThat(handler.completedText).isEqualTo(expectedPrompt.instruction());
        assertThat(handler.errorMessage).isNull();
    }

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void resolvesEachSupportedOption(String option, CharacterSpeechPrompt expectedPrompt) {
        assertThat(CharacterSpeechPrompt.fromOption("  " + option.toUpperCase() + "  "))
                .isEqualTo(Optional.of(expectedPrompt));
    }

    @Test
    void skipsBlankCharacterSpeechRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        CharacterSpeechLlmClient characterSpeechLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        CharacterSpeechQuickActionService service = new CharacterSpeechQuickActionService(characterSpeechLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new QuickActionStreamRequest("   ", "de-DE", "direct_speech", null), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAMissingOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        CharacterSpeechLlmClient characterSpeechLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        CharacterSpeechQuickActionService service = new CharacterSpeechQuickActionService(characterSpeechLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Projekt ist freigegeben. Team startet am Montag.", "de-DE", null, null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Character-Speech-Option ist erforderlich.");
    }

    @Test
    void reportsAnInvalidOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        CharacterSpeechLlmClient characterSpeechLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        CharacterSpeechQuickActionService service = new CharacterSpeechQuickActionService(characterSpeechLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Projekt ist freigegeben. Team startet am Montag.", "de-DE", "narrator", null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Character-Speech-Option ist ungueltig.");
    }

    @Test
    void reportsACharacterSpeechSpecificErrorWhenStreamingFails() {
        CharacterSpeechLlmClient characterSpeechLlmClient = (text, language, prompt) -> {
            throw new IllegalStateException("boom");
        };
        CharacterSpeechQuickActionService service = new CharacterSpeechQuickActionService(characterSpeechLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Projekt ist freigegeben. Team startet am Montag.", "de-DE", "indirect_speech", null),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Character-Speech-Text konnte nicht erstellt werden.");
    }

    private static Stream<Object[]> supportedPrompts() {
        return Stream.of(
                new Object[]{"direct_speech", CharacterSpeechPrompt.DIRECT_SPEECH},
                new Object[]{"indirect_speech", CharacterSpeechPrompt.INDIRECT_SPEECH}
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
