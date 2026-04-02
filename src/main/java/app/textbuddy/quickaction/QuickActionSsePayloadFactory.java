package app.textbuddy.quickaction;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class QuickActionSsePayloadFactory {

    private static final String DEFAULT_ERROR_MESSAGE = "Quick Action konnte nicht abgeschlossen werden.";

    public ChunkPayload chunk(String text) {
        return new ChunkPayload(normalize(text));
    }

    public CompletePayload complete(String text) {
        return new CompletePayload(normalize(text));
    }

    public ErrorPayload error(String message) {
        String normalized = normalize(message);
        return new ErrorPayload(normalized.isBlank() ? DEFAULT_ERROR_MESSAGE : normalized);
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "");
    }

    public record ChunkPayload(String text) {
    }

    public record CompletePayload(String text) {
    }

    public record ErrorPayload(String message) {
    }
}
