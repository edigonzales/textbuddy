package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.CustomQuickActionRequestValidator;
import app.textbuddy.quickaction.CustomQuickActionService;
import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/quick-actions")
public class CustomQuickActionController {

    private static final String DEFAULT_ERROR_MESSAGE = "Custom-Stream konnte nicht gestartet werden.";

    private final CustomQuickActionService customQuickActionService;
    private final CustomQuickActionRequestValidator requestValidator;
    private final QuickActionSsePayloadFactory payloadFactory;

    public CustomQuickActionController(
            CustomQuickActionService customQuickActionService,
            CustomQuickActionRequestValidator requestValidator,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.customQuickActionService = customQuickActionService;
        this.requestValidator = requestValidator;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/custom/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCustom(@RequestBody QuickActionStreamRequest request) {
        requestValidator.validateOrThrow(request);

        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory);

        Thread.startVirtualThread(() -> {
            try {
                customQuickActionService.stream(request, writer);
            } catch (RuntimeException exception) {
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }
}
