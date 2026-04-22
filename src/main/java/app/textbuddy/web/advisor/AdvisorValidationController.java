package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorValidateRequest;
import app.textbuddy.advisor.AdvisorValidationService;
import app.textbuddy.web.error.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorValidationController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorValidationController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Advisor-Validierung konnte nicht gestartet werden.";

    private final AdvisorValidationService advisorValidationService;
    private final AdvisorRoleAccessService advisorRoleAccessService;

    public AdvisorValidationController(
            AdvisorValidationService advisorValidationService,
            AdvisorRoleAccessService advisorRoleAccessService
    ) {
        this.advisorValidationService = advisorValidationService;
        this.advisorRoleAccessService = advisorRoleAccessService;
    }

    @PostMapping(path = "/validate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter validate(
            @RequestBody AdvisorValidateRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        advisorRoleAccessService.assertValidationAccess(request == null ? java.util.List.of() : request.docs(), authentication);

        String traceId = TraceIdSupport.resolve(httpServletRequest);
        SseEmitter emitter = new SseEmitter(0L);
        AdvisorValidationSseEmitterWriter writer = new AdvisorValidationSseEmitterWriter(emitter, traceId);

        Thread.startVirtualThread(() -> {
            try {
                advisorValidationService.validate(request, writer);
            } catch (RuntimeException exception) {
                log.error("[{}] Advisor validation stream failed.", traceId, exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });

        return emitter;
    }
}
