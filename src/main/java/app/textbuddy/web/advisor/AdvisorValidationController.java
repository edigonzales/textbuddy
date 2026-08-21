package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorValidateRequest;
import app.textbuddy.advisor.AdvisorValidationService;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorValidationController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorValidationController.class);
    private static final String DEFAULT_ERROR_MESSAGE = "Advisor-Validierung konnte nicht gestartet werden.";
    private static final long STREAM_TIMEOUT_MILLIS = 120_000L;

    private final AdvisorValidationService advisorValidationService;
    private final RequestInputValidator inputValidator;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AdvisorValidationController(
            AdvisorValidationService advisorValidationService,
            RequestInputValidator inputValidator
    ) {
        this.advisorValidationService = advisorValidationService;
        this.inputValidator = inputValidator;
    }

    @PostMapping(path = "/validate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter validate(
            @RequestBody AdvisorValidateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        inputValidator.text(request == null ? null : request.text());

        String traceId = TraceIdSupport.resolve(httpServletRequest);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AdvisorValidationSseEmitterWriter writer = new AdvisorValidationSseEmitterWriter(emitter, traceId);
        AtomicReference<Future<?>> task = new AtomicReference<>();

        Future<?> future = executor.submit(() -> {
            try {
                advisorValidationService.validate(request, writer);
            } catch (RuntimeException exception) {
                log.error("[{}] Advisor validation stream failed.", traceId, exception);
                writer.error(DEFAULT_ERROR_MESSAGE);
            }
        });
        task.set(future);

        Runnable cancel = () -> {
            Future<?> runningTask = task.get();
            if (runningTask != null && !runningTask.isDone()) {
                runningTask.cancel(true);
            }
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());

        return emitter;
    }

    @PreDestroy
    void closeExecutor() {
        executor.close();
    }
}
