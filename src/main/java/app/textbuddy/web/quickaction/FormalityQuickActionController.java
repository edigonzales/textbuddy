package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.FormalityPrompt;
import app.textbuddy.quickaction.FormalityQuickActionRequest;
import app.textbuddy.quickaction.FormalityQuickActionService;
import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/quick-actions")
public class FormalityQuickActionController {

    private static final Logger log = LoggerFactory.getLogger(FormalityQuickActionController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Formality-Stream konnte nicht gestartet werden.";
    private static final String MISSING_OPTION_MESSAGE = "Formality-Option ist erforderlich.";
    private static final String INVALID_OPTION_MESSAGE = "Formality-Option ist ungueltig.";

    private final FormalityQuickActionService formalityQuickActionService;
    private final QuickActionSsePayloadFactory payloadFactory;

    public FormalityQuickActionController(
            FormalityQuickActionService formalityQuickActionService,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.formalityQuickActionService = formalityQuickActionService;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/formality/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFormality(@RequestBody FormalityQuickActionRequest request) {
        validateOption(request);

        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory);

        Thread.startVirtualThread(() -> {
            try {
                formalityQuickActionService.stream(request, writer);
            } catch (RuntimeException exception) {
                log.error("Formality stream failed.", exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }

    private void validateOption(FormalityQuickActionRequest request) {
        String option = request == null ? null : request.option();
        String normalized = option == null ? "" : option.trim();

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_OPTION_MESSAGE);
        }

        if (FormalityPrompt.fromOption(normalized).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OPTION_MESSAGE);
        }
    }
}
