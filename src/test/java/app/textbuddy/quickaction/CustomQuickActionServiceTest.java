package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.CustomLlmClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CustomQuickActionServiceTest {

    @Test
    void streamsCustomTextAndPassesThePreparedPromptToTheLlm() {
        AtomicReference<CustomQuickActionPrompt> capturedPrompt = new AtomicReference<>();
        CustomLlmClient customLlmClient = (text, language, prompt) -> {
            capturedPrompt.set(prompt);
            return List.of(
                    "Custom Rewrite\n\n",
                    "Auftrag: ",
                    prompt.userPrompt()
            );
        };
        CustomQuickActionService service = new CustomQuickActionService(
                customLlmClient,
                new CustomQuickActionRequestValidator()
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest(
                        "Projektstart ist morgen.",
                        "de-DE",
                        null,
                        "  Formuliere den Text als interne Ankuendigung.  "
                ),
                handler
        );

        assertThat(capturedPrompt.get()).isNotNull();
        assertThat(capturedPrompt.get().userPrompt()).isEqualTo("Formuliere den Text als interne Ankuendigung.");
        assertThat(capturedPrompt.get().instruction()).contains("Arbeitsauftrag:");
        assertThat(handler.chunks).containsExactly(
                "Custom Rewrite\n\n",
                "Auftrag: ",
                "Formuliere den Text als interne Ankuendigung."
        );
        assertThat(handler.completedText)
                .isEqualTo("Custom Rewrite\n\nAuftrag: Formuliere den Text als interne Ankuendigung.");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void skipsBlankCustomRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        CustomLlmClient customLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        CustomQuickActionService service = new CustomQuickActionService(
                customLlmClient,
                new CustomQuickActionRequestValidator()
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("   ", "de-DE", null, "Bitte als interne Ankuendigung umschreiben."),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAMissingPromptWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        CustomLlmClient customLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        CustomQuickActionService service = new CustomQuickActionService(
                customLlmClient,
                new CustomQuickActionRequestValidator()
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Projektstart ist morgen.", "de-DE", null, null),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Custom-Prompt ist erforderlich.");
    }

    @Test
    void reportsAnInvalidPromptWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        CustomLlmClient customLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        CustomQuickActionService service = new CustomQuickActionService(
                customLlmClient,
                new CustomQuickActionRequestValidator()
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Projektstart ist morgen.", "de-DE", null, "x".repeat(401)),
                handler
        );

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Custom-Prompt ist ungueltig.");
    }

    @Test
    void reportsACustomSpecificErrorWhenStreamingFails() {
        CustomLlmClient customLlmClient = (text, language, prompt) -> {
            throw new IllegalStateException("boom");
        };
        CustomQuickActionService service = new CustomQuickActionService(
                customLlmClient,
                new CustomQuickActionRequestValidator()
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest(
                        "Projektstart ist morgen.",
                        "de-DE",
                        null,
                        "Formuliere den Text als interne Ankuendigung."
                ),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Custom-Rewrite konnte nicht erstellt werden.");
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
