package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;

final class QuickActionSseEmitterWriter implements QuickActionStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(QuickActionSseEmitterWriter.class);

    private final SseEmitter emitter;
    private final QuickActionSsePayloadFactory payloadFactory;
    private final String traceId;
    private int chunkCount;

    QuickActionSseEmitterWriter(SseEmitter emitter, QuickActionSsePayloadFactory payloadFactory, String traceId) {
        this.emitter = emitter;
        this.payloadFactory = payloadFactory;
        this.traceId = traceId == null ? "" : traceId.trim();
    }

    @Override
    public void chunk(String text) {
        chunkCount += 1;
        send("chunk", payloadFactory.chunk(text));
    }

    @Override
    public void complete(String text) {
        try {
            send("complete", payloadFactory.complete(text));
            log.info("[{}] Quick-Action-SSE abgeschlossen (chunks={}).", traceId, chunkCount);
            emitter.complete();
        } catch (RuntimeException exception) {
            log.warn("[{}] Failed to finish quick action SSE response.", traceId, exception);
            emitter.completeWithError(exception);
        }
    }

    @Override
    public void error(String message) {
        try {
            send("error", payloadFactory.error(message));
            log.warn("[{}] Quick-Action-SSE meldet Fehler: {}", traceId, message);
            emitter.complete();
        } catch (RuntimeException exception) {
            log.warn("[{}] Failed to send quick action SSE error response.", traceId, exception);
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
