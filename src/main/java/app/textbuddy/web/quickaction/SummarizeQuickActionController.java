package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamRequest;
import app.textbuddy.quickaction.SummarizePrompt;
import app.textbuddy.quickaction.SummarizeQuickActionService;
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
public class SummarizeQuickActionController {

    private static final Logger log = LoggerFactory.getLogger(SummarizeQuickActionController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Summarize-Stream konnte nicht gestartet werden.";
    private static final String MISSING_OPTION_MESSAGE = "Summarize-Option ist erforderlich.";
    private static final String INVALID_OPTION_MESSAGE = "Summarize-Option ist ungueltig.";

    private final SummarizeQuickActionService summarizeQuickActionService;
    private final QuickActionSsePayloadFactory payloadFactory;

    public SummarizeQuickActionController(
            SummarizeQuickActionService summarizeQuickActionService,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.summarizeQuickActionService = summarizeQuickActionService;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/summarize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSummarize(@RequestBody QuickActionStreamRequest request) {
        validateOption(request);

        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory);

        Thread.startVirtualThread(() -> {
            try {
                summarizeQuickActionService.stream(request, writer);
            } catch (RuntimeException exception) {
                log.error("Summarize stream failed.", exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }

    private void validateOption(QuickActionStreamRequest request) {
        String option = request == null ? null : request.option();
        String normalized = option == null ? "" : option.trim();

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_OPTION_MESSAGE);
        }

        if (SummarizePrompt.fromOption(normalized).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OPTION_MESSAGE);
        }
    }
}
