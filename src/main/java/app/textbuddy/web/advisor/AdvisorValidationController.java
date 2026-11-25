package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorFixRequest;
import app.textbuddy.advisor.AdvisorService;
import app.textbuddy.advisor.AdvisorValidateRequest;
import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.web.error.TraceIdSupport;
import app.textbuddy.web.RequestInputValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorValidationController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorValidationController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Advisor-Validierung konnte nicht gestartet werden.";
    private final AdvisorService advisorService;
    private final RequestInputValidator inputValidator;
    private final long streamTimeoutMillis;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AdvisorValidationController(
            AdvisorService advisorService,
            RequestInputValidator inputValidator,
            TextbuddyProperties properties
    ) {
        this.advisorService = advisorService;
        this.inputValidator = inputValidator;
        this.streamTimeoutMillis = AdvisorService.maximumValidationDuration(
                properties.getLlm().normalizedTimeout()
        ).toMillis();
    }

    @PostMapping(path = "/validate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter validate(
            @RequestBody AdvisorValidateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        inputValidator.text(request == null ? null : request.text());

        String traceId = TraceIdSupport.resolve(httpServletRequest);
        SseEmitter emitter = new SseEmitter(streamTimeoutMillis);
        AdvisorValidationSseEmitterWriter writer = new AdvisorValidationSseEmitterWriter(emitter, traceId);

        Future<?> future = executor.submit(() -> {
            try {
                advisorService.validate(request, writer);
            } catch (RuntimeException exception) {
                if (Thread.currentThread().isInterrupted()) return;
                log.warn("[{}] Advisor validation stream failed ({}).", traceId, exception.getClass().getSimpleName());
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });
        Runnable cancel = () -> {
            if (!future.isDone()) future.cancel(true);
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());

        return emitter;
    }

    @PostMapping(path = "/fix", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> fix(@RequestBody AdvisorFixRequest request) {
        inputValidator.text(request == null ? null : request.text());
        String suggestions = request == null || request.findings() == null
                ? ""
                : request.findings().stream()
                        .filter(Objects::nonNull)
                        .map(finding -> Objects.requireNonNullElse(finding.suggestion(), ""))
                        .collect(Collectors.joining());
        inputValidator.prompt(suggestions);
        return advisorService.fix(request);
    }

    @PreDestroy
    void closeExecutor() {
        executor.shutdownNow();
    }
}
