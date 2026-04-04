package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorValidateRequest;
import app.textbuddy.advisor.AdvisorValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorValidationController {

    private static final String DEFAULT_ERROR_MESSAGE = "Advisor-Validierung konnte nicht gestartet werden.";

    private final AdvisorValidationService advisorValidationService;

    public AdvisorValidationController(AdvisorValidationService advisorValidationService) {
        this.advisorValidationService = advisorValidationService;
    }

    @PostMapping(path = "/validate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter validate(@RequestBody AdvisorValidateRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        AdvisorValidationSseEmitterWriter writer = new AdvisorValidationSseEmitterWriter(emitter);

        Thread.startVirtualThread(() -> {
            try {
                advisorValidationService.validate(request, writer);
            } catch (RuntimeException exception) {
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }
}
