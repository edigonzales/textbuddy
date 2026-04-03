package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.ProofreadLlmClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ProofreadQuickActionServiceTest {

    @Test
    void streamsProofreadTextAndBuildsTheCompleteRewrite() {
        ProofreadLlmClient proofreadLlmClient = (text, language) -> List.of(
                "This is ",
                "the corrected text."
        );
        ProofreadQuickActionService service = new ProofreadQuickActionService(proofreadLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("This is teh corrected text.", "en-US", null, null),
                handler
        );

        assertThat(handler.chunks).containsExactly("This is ", "the corrected text.");
        assertThat(handler.completedText).isEqualTo("This is the corrected text.");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void skipsBlankProofreadRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        ProofreadLlmClient proofreadLlmClient = (text, language) -> {
            called.set(true);
            return List.of();
        };
        ProofreadQuickActionService service = new ProofreadQuickActionService(proofreadLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(new QuickActionStreamRequest("   ", "en-US", null, null), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsAProofreadSpecificErrorWhenStreamingFails() {
        ProofreadLlmClient proofreadLlmClient = (text, language) -> {
            throw new IllegalStateException("boom");
        };
        ProofreadQuickActionService service = new ProofreadQuickActionService(proofreadLlmClient);
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.stream(
                new QuickActionStreamRequest("This is teh text.", "en-US", null, null),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Proofread konnte nicht erstellt werden.");
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
