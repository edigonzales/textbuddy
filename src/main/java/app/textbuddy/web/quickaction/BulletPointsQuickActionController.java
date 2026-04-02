package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.QuickActionService;
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
public class BulletPointsQuickActionController {

    private static final String DEFAULT_ERROR_MESSAGE = "Bullet-Points-Stream konnte nicht gestartet werden.";

    private final QuickActionService quickActionService;
    private final QuickActionSsePayloadFactory payloadFactory;

    public BulletPointsQuickActionController(
            QuickActionService quickActionService,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.quickActionService = quickActionService;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/bullet-points/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBulletPoints(@RequestBody QuickActionStreamRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory);

        Thread.startVirtualThread(() -> {
            try {
                quickActionService.streamBulletPoints(request, writer);
            } catch (RuntimeException exception) {
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }
}
