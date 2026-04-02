package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamHandler;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;

final class QuickActionSseEmitterWriter implements QuickActionStreamHandler {

    private final SseEmitter emitter;
    private final QuickActionSsePayloadFactory payloadFactory;

    QuickActionSseEmitterWriter(SseEmitter emitter, QuickActionSsePayloadFactory payloadFactory) {
        this.emitter = emitter;
        this.payloadFactory = payloadFactory;
    }

    @Override
    public void chunk(String text) {
        send("chunk", payloadFactory.chunk(text));
    }

    @Override
    public void complete(String text) {
        try {
            send("complete", payloadFactory.complete(text));
            emitter.complete();
        } catch (RuntimeException exception) {
            emitter.completeWithError(exception);
        }
    }

    @Override
    public void error(String message) {
        try {
            send("error", payloadFactory.error(message));
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
