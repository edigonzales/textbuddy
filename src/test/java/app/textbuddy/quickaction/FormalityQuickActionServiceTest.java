package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.FormalityLlmClient;
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

class FormalityQuickActionServiceTest {

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void mapsEachSupportedOptionToItsPrompt(String option, FormalityPrompt expectedPrompt) {
        AtomicReference<FormalityPrompt> capturedPrompt = new AtomicReference<>();
        FormalityLlmClient formalityLlmClient = (text, language, prompt) -> {
            capturedPrompt.set(prompt);
            return List.of(prompt.instruction());
        };
        FormalityQuickActionService service = new FormalityQuickActionService(formalityLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new FormalityQuickActionRequest("Hallo, wir brauchen schnell eine Rueckmeldung.", "de-DE", option),
                handler
        );

        assertThat(capturedPrompt.get()).isEqualTo(expectedPrompt);
        assertThat(handler.completedText).isEqualTo(expectedPrompt.instruction());
        assertThat(handler.errorMessage).isNull();
    }

    @ParameterizedTest
    @MethodSource("supportedPrompts")
    void resolvesEachSupportedOption(String option, FormalityPrompt expectedPrompt) {
        assertThat(FormalityPrompt.fromOption("  " + option.toUpperCase() + "  "))
                .isEqualTo(Optional.of(expectedPrompt));
    }

    @Test
    void skipsBlankFormalityRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        FormalityLlmClient formalityLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        FormalityQuickActionService service = new FormalityQuickActionService(formalityLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new FormalityQuickActionRequest("   ", "de-DE", "formal"), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAMissingOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        FormalityLlmClient formalityLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        FormalityQuickActionService service = new FormalityQuickActionService(formalityLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new FormalityQuickActionRequest("Hallo, wir brauchen schnell eine Rueckmeldung.", "de-DE", null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Formality-Option ist erforderlich.");
    }

    @Test
    void reportsAnInvalidOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        FormalityLlmClient formalityLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        FormalityQuickActionService service = new FormalityQuickActionService(formalityLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new FormalityQuickActionRequest("Hallo, wir brauchen schnell eine Rueckmeldung.", "de-DE", "casual"),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Formality-Option ist ungültig.");
    }

    @Test
    void reportsAFormalitySpecificErrorWhenStreamingFails() {
        FormalityLlmClient formalityLlmClient = (text, language, prompt) -> {
            throw new IllegalStateException("boom");
        };
        FormalityQuickActionService service = new FormalityQuickActionService(formalityLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new FormalityQuickActionRequest("Hallo, wir brauchen schnell eine Rueckmeldung.", "de-DE", "formal"),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Formality-Rewrite konnte nicht erstellt werden.");
    }

    private static Stream<Object[]> supportedPrompts() {
        return Stream.of(
                new Object[]{"formal", FormalityPrompt.FORMAL},
                new Object[]{"informal", FormalityPrompt.INFORMAL}
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
