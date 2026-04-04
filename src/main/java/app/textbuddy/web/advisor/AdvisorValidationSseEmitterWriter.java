package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorValidationEvent;
import app.textbuddy.advisor.AdvisorValidationStreamHandler;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

final class AdvisorValidationSseEmitterWriter implements AdvisorValidationStreamHandler {

    private final SseEmitter emitter;

    AdvisorValidationSseEmitterWriter(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void validation(AdvisorValidationEvent event) {
        send("validation", event);
    }

    @Override
    public void complete() {
        emitter.complete();
    }

    @Override
    public void error(String message) {
        try {
            send("error", Map.of("message", message));
            emitter.complete();
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void send(String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
