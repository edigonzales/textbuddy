package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.QuickActionService;
import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamRequest;
import app.textbuddy.web.error.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
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
public class PlainLanguageQuickActionController {

    private static final Logger log = LoggerFactory.getLogger(PlainLanguageQuickActionController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Plain-Language-Stream konnte nicht gestartet werden.";

    private final QuickActionService quickActionService;
    private final QuickActionSsePayloadFactory payloadFactory;

    public PlainLanguageQuickActionController(
            QuickActionService quickActionService,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.quickActionService = quickActionService;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/plain-language/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPlainLanguage(@RequestBody QuickActionStreamRequest request, HttpServletRequest httpServletRequest) {
        String traceId = TraceIdSupport.resolve(httpServletRequest);
        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory, traceId);

        Thread.startVirtualThread(() -> {
            try {
                quickActionService.streamPlainLanguage(request, writer);
            } catch (RuntimeException exception) {
                log.error("[{}] Plain Language stream failed.", traceId, exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }
}
