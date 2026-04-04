package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.ProofreadQuickActionService;
import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/quick-actions")
public class ProofreadQuickActionController {

    private static final Logger log = LoggerFactory.getLogger(ProofreadQuickActionController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Proofread-Stream konnte nicht gestartet werden.";

    private final ProofreadQuickActionService proofreadQuickActionService;
    private final QuickActionSsePayloadFactory payloadFactory;

    public ProofreadQuickActionController(
            ProofreadQuickActionService proofreadQuickActionService,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.proofreadQuickActionService = proofreadQuickActionService;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/proofread/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProofread(@RequestBody QuickActionStreamRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory);

        Thread.startVirtualThread(() -> {
            try {
                proofreadQuickActionService.stream(request, writer);
            } catch (RuntimeException exception) {
                log.error("Proofread stream failed.", exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }
}
