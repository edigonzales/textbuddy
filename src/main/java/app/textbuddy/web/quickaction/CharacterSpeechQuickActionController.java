package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.CharacterSpeechPrompt;
import app.textbuddy.quickaction.CharacterSpeechQuickActionService;
import app.textbuddy.quickaction.QuickActionSsePayloadFactory;
import app.textbuddy.quickaction.QuickActionStreamRequest;
import app.textbuddy.web.error.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
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
public class CharacterSpeechQuickActionController {

    private static final Logger log = LoggerFactory.getLogger(CharacterSpeechQuickActionController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Character-Speech-Stream konnte nicht gestartet werden.";
    private static final String MISSING_OPTION_MESSAGE = "Character-Speech-Option ist erforderlich.";
    private static final String INVALID_OPTION_MESSAGE = "Character-Speech-Option ist ungültig.";

    private final CharacterSpeechQuickActionService characterSpeechQuickActionService;
    private final QuickActionSsePayloadFactory payloadFactory;

    public CharacterSpeechQuickActionController(
            CharacterSpeechQuickActionService characterSpeechQuickActionService,
            QuickActionSsePayloadFactory payloadFactory
    ) {
        this.characterSpeechQuickActionService = characterSpeechQuickActionService;
        this.payloadFactory = payloadFactory;
    }

    @PostMapping(path = "/character-speech/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCharacterSpeech(
            @RequestBody QuickActionStreamRequest request,
            HttpServletRequest httpServletRequest
    ) {
        validateOption(request);

        String traceId = TraceIdSupport.resolve(httpServletRequest);
        SseEmitter emitter = new SseEmitter(0L);
        QuickActionSseEmitterWriter writer = new QuickActionSseEmitterWriter(emitter, payloadFactory, traceId);

        Thread.startVirtualThread(() -> {
            try {
                characterSpeechQuickActionService.stream(request, writer);
            } catch (RuntimeException exception) {
                log.error("[{}] Character Speech stream failed.", traceId, exception);
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

        if (CharacterSpeechPrompt.fromOption(normalized).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OPTION_MESSAGE);
        }
    }
}
