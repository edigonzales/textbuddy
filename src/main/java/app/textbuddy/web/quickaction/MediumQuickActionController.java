package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.MediumPrompt;
import app.textbuddy.quickaction.MediumCurrentUserResolver;
import app.textbuddy.quickaction.MediumQuickActionRequest;
import app.textbuddy.quickaction.MediumQuickActionService;
import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.web.error.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/quick-actions")
public class MediumQuickActionController {

    private static final Logger log = LoggerFactory.getLogger(MediumQuickActionController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Medium-Stream konnte nicht gestartet werden.";
    private static final String MISSING_OPTION_MESSAGE = "Medium-Option ist erforderlich.";
    private static final String INVALID_OPTION_MESSAGE = "Medium-Option ist ungültig.";

    private final MediumQuickActionService mediumQuickActionService;
    private final MediumCurrentUserResolver mediumCurrentUserResolver;
    private final QuickActionSsePayloadFactory payloadFactory;

    public MediumQuickActionController(
            MediumQuickActionService mediumQuickActionService,
            MediumCurrentUserResolver mediumCurrentUserResolver,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.mediumQuickActionService = mediumQuickActionService;
        this.mediumCurrentUserResolver = mediumCurrentUserResolver;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/medium/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMedium(
            @RequestBody MediumQuickActionRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        validateOption(request);
        var currentUser = mediumCurrentUserResolver.resolve(authentication);

        String traceId = TraceIdSupport.resolve(httpServletRequest);
        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory, traceId);

        Thread.startVirtualThread(() -> {
            try {
                mediumQuickActionService.stream(request, currentUser, writer);
            } catch (RuntimeException exception) {
                log.error("[{}] Medium stream failed.", traceId, exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }

    private void validateOption(MediumQuickActionRequest request) {
        String option = request == null ? null : request.option();
        String normalized = option == null ? "" : option.trim();

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_OPTION_MESSAGE);
        }

        if (MediumPrompt.fromOption(normalized).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OPTION_MESSAGE);
        }
    }
}
