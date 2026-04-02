package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.BulletPointsLlmClient;
import app.textbuddy.integration.llm.PlainLanguageLlmClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultQuickActionServiceTest {

    @Test
    void streamsBulletPointsAndBuildsTheCompleteRewrite() {
        PlainLanguageLlmClient plainLanguageLlmClient = (text, language) -> List.of();
        BulletPointsLlmClient bulletPointsLlmClient = (text, language) -> List.of(
                "- Erster Punkt.\n",
                "- Zweiter Punkt."
        );
        DefaultQuickActionService service = new DefaultQuickActionService(
                plainLanguageLlmClient,
                bulletPointsLlmClient
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.streamBulletPoints(
                new QuickActionStreamRequest("Erster Punkt. Zweiter Punkt.", "de-DE", null, null),
                handler
        );

        assertThat(handler.chunks).containsExactly("- Erster Punkt.\n", "- Zweiter Punkt.");
        assertThat(handler.completedText).isEqualTo("- Erster Punkt.\n- Zweiter Punkt.");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void skipsBlankBulletPointRequestsWithoutCallingTheLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        PlainLanguageLlmClient plainLanguageLlmClient = (text, language) -> List.of();
        BulletPointsLlmClient bulletPointsLlmClient = (text, language) -> {
            called.set(true);
            return List.of();
        };
        DefaultQuickActionService service = new DefaultQuickActionService(
                plainLanguageLlmClient,
                bulletPointsLlmClient
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.streamBulletPoints(new QuickActionStreamRequest("   ", "de-DE", null, null), handler);

        assertThat(called).isFalse();
        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isEqualTo("");
        assertThat(handler.errorMessage).isNull();
    }

    @Test
    void reportsABulletPointSpecificErrorWhenStreamingFails() {
        PlainLanguageLlmClient plainLanguageLlmClient = (text, language) -> List.of();
        BulletPointsLlmClient bulletPointsLlmClient = (text, language) -> {
            throw new IllegalStateException("boom");
        };
        DefaultQuickActionService service = new DefaultQuickActionService(
                plainLanguageLlmClient,
                bulletPointsLlmClient
        );
        RecordingQuickActionStreamHandler handler = new RecordingQuickActionStreamHandler();

        service.streamBulletPoints(
                new QuickActionStreamRequest("Ein Punkt.", "de-DE", null, null),
                handler
        );

        assertThat(handler.chunks).isEmpty();
        assertThat(handler.completedText).isNull();
        assertThat(handler.errorMessage).isEqualTo("Bullet-Points-Rewrite konnte nicht erstellt werden.");
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
