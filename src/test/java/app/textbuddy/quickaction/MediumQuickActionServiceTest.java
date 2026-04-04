package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.MediumLlmClient;
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

class MediumQuickActionServiceTest {

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void mapsEachSupportedOptionToItsPrompt(String option, MediumPrompt expectedPrompt) {
        AtomicReference<MediumPrompt> capturedPrompt = new AtomicReference<>();
        MediumLlmClient mediumLlmClient = (text, language, prompt) -> {
            capturedPrompt.set(prompt);
            return List.of(prompt.instruction());
        };
        MediumQuickActionService service = new MediumQuickActionService(mediumLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new MediumQuickActionRequest("Projekt ist freigegeben. Umsetzung startet jetzt.", "de-DE", option),
                handler
        );

        assertThat(capturedPrompt.get()).isEqualTo(expectedPrompt);
        assertThat(handler.completedText).isEqualTo(expectedPrompt.instruction());
        assertThat(handler.errorMessage).isNull();
    }

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void resolvesEachSupportedOption(String option, MediumPrompt expectedPrompt) {
        assertThat(MediumPrompt.fromOption("  " + option.toUpperCase() + "  "))
                .isEqualTo(Optional.of(expectedPrompt));
    }

    @Test
    void skipsBlankMediumRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        MediumLlmClient mediumLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        MediumQuickActionService service = new MediumQuickActionService(mediumLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new MediumQuickActionRequest("   ", "de-DE", "email"), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAMissingOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        MediumLlmClient mediumLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        MediumQuickActionService service = new MediumQuickActionService(mediumLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new MediumQuickActionRequest("Projekt ist freigegeben. Umsetzung startet jetzt.", "de-DE", null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Medium-Option ist erforderlich.");
    }

    @Test
    void reportsAnInvalidOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        MediumLlmClient mediumLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        MediumQuickActionService service = new MediumQuickActionService(mediumLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new MediumQuickActionRequest("Projekt ist freigegeben. Umsetzung startet jetzt.", "de-DE", "memo"),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Medium-Option ist ungueltig.");
    }

    @Test
    void reportsAMediumSpecificErrorWhenStreamingFails() {
        MediumLlmClient mediumLlmClient = (text, language, prompt) -> {
            throw new IllegalStateException("boom");
        };
        MediumQuickActionService service = new MediumQuickActionService(mediumLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new MediumQuickActionRequest("Projekt ist freigegeben. Umsetzung startet jetzt.", "de-DE", "report"),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Medium-Text konnte nicht erstellt werden.");
    }

    private static Stream<Object[]> supportedPrompts() {
        return Stream.of(
                new Object[]{"email", MediumPrompt.EMAIL},
                new Object[]{"official_letter", MediumPrompt.OFFICIAL_LETTER},
                new Object[]{"presentation", MediumPrompt.PRESENTATION},
                new Object[]{"report", MediumPrompt.REPORT}
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
