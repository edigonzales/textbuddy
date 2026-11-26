package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorValidationEvent;
import app.textbuddy.advisor.AdvisorProgressEvent;
import app.textbuddy.advisor.AdvisorValidationStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

final class AdvisorValidationSseEmitterWriter implements AdvisorValidationStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(AdvisorValidationSseEmitterWriter.class);

    private final SseEmitter emitter;
    private final String traceId;

    AdvisorValidationSseEmitterWriter(SseEmitter emitter, String traceId) {
        this.emitter = emitter;
        this.traceId = traceId == null ? "" : traceId.trim();
    }

    @Override
    public void validation(AdvisorValidationEvent event) {
        send("validation", event);
    }

    @Override
    public void progress(AdvisorProgressEvent event) {
        send("progress", event);
    }

    @Override
    public void complete(boolean incomplete) {
        send("complete", Map.of("incomplete", incomplete));
        emitter.complete();
    }

    void error(String message) {
        try {
            send("error", Map.of("message", message));
            log.warn("[{}] Advisor-SSE meldet Fehler: {}", traceId, message);
            emitter.complete();
        } catch (RuntimeException exception) {
            log.debug("[{}] Failed to send advisor SSE error response.", traceId);
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
