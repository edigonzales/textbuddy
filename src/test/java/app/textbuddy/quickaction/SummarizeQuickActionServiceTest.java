package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.SummarizeLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SummarizeQuickActionServiceTest {

    @ParameterizedTest
    @MethodSource("supportedOptionMappings")
    void mapsEachSupportedOptionToItsPrompt(String option, SummarizePrompt expectedPrompt) {
        AtomicReference<SummarizePrompt> capturedPrompt = new AtomicReference<>();
        SummarizeLlmClient summarizeLlmClient = (text, language, prompt) -> {
            capturedPrompt.set(prompt);
            return List.of("Zusammenfassung fuer ", prompt.option());
        };
        SummarizeQuickActionService service = new SummarizeQuickActionService(summarizeLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Alpha. Beta. Gamma. Delta.", "de-DE", option, null),
                handler
        );

        assertThat(capturedPrompt.get()).isEqualTo(expectedPrompt);
        assertThat(handler.completedText).isEqualTo("Zusammenfassung fuer " + expectedPrompt.option());
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void skipsBlankSummarizeRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        SummarizeLlmClient summarizeLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        SummarizeQuickActionService service = new SummarizeQuickActionService(summarizeLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new QuickActionStreamRequest("   ", "de-DE", "sentence", null), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAMissingOptionWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        SummarizeLlmClient summarizeLlmClient = (text, language, prompt) -> {
            called.set(true);
            return List.of();
        };
        SummarizeQuickActionService service = new SummarizeQuickActionService(summarizeLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new QuickActionStreamRequest("Alpha. Beta.", "de-DE", null, null), handler);

        assertThat(called).isFalse();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Summarize-Option ist erforderlich.");
    }

    @Test
    void reportsASummarizeSpecificErrorWhenStreamingFails() {
        SummarizeLlmClient summarizeLlmClient = (text, language, prompt) -> {
            throw new IllegalStateException("boom");
        };
        SummarizeQuickActionService service = new SummarizeQuickActionService(summarizeLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("Alpha. Beta.", "de-DE", "paragraph", null),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Zusammenfassung konnte nicht erstellt werden.");
    }

    private static Stream<Object[]> supportedOptionMappings() {
        return Stream.of(
                new Object[]{"sentence", SummarizePrompt.SENTENCE},
                new Object[]{"three_sentence", SummarizePrompt.THREE_SENTENCE},
                new Object[]{"paragraph", SummarizePrompt.PARAGRAPH},
                new Object[]{"page", SummarizePrompt.PAGE},
                new Object[]{"management_summary", SummarizePrompt.MANAGEMENT_SUMMARY}
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
