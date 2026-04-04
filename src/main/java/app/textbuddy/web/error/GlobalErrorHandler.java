package app.textbuddy.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@ControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final ApiErrorResponseFactory errorResponseFactory;

    public GlobalErrorHandler(ApiErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(Exception.class)
    public Object handle(Exception exception, HttpServletRequest request) {
        HttpStatusCode status = resolveStatus(exception);
        String detail = resolveDetail(exception, status);

        logException(exception, request, status, detail);

        if (isApiRequest(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(errorResponseFactory.create(status, detail, request));
        }

        return buildErrorPage(status, detail, request);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.startsWith("/api/");
    }

    private HttpStatusCode resolveStatus(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode();
        }

        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                exception.getClass(),
                ResponseStatus.class
        );

        if (responseStatus != null) {
            HttpStatus annotatedStatus = responseStatus.code() != HttpStatus.INTERNAL_SERVER_ERROR
                    ? responseStatus.code()
                    : responseStatus.value();

            if (annotatedStatus != HttpStatus.INTERNAL_SERVER_ERROR || responseStatus.reason().isBlank()) {
                return annotatedStatus;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveDetail(Exception exception, HttpStatusCode status) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            String reason = responseStatusException.getReason();

            if (reason != null && !reason.isBlank()) {
                return reason;
            }
        }

        if (exception instanceof ErrorResponse errorResponse) {
            String detail = errorResponse.getBody().getDetail();

            if (detail != null
                    && !detail.isBlank()
                    && !status.is5xxServerError()
                    && status.value() != 404
                    && status.value() != 405) {
                return detail;
            }
        }

        if (hasResponseStatusAnnotation(exception)
                && exception.getMessage() != null
                && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        if (status.value() == 404 || status.value() == 405) {
            return defaultMessage(status);
        }

        if (!status.is5xxServerError() && exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return defaultMessage(status);
    }

    private String defaultMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Die Anfrage ist ungueltig.";
            case 401 -> "Anmeldung erforderlich.";
            case 403 -> "Zugriff verweigert.";
            case 404 -> "Die angeforderte Ressource wurde nicht gefunden.";
            case 405 -> "Diese HTTP-Methode wird fuer den Endpoint nicht unterstuetzt.";
            case 502 -> "Ein angebundener Dienst hat ungueltig geantwortet.";
            default -> "Ein interner Fehler ist aufgetreten.";
        };
    }

    private void logException(
            Exception exception,
            HttpServletRequest request,
            HttpStatusCode status,
            String detail
    ) {
        String traceId = TraceIdSupport.resolve(request);
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (status.is5xxServerError()) {
            log.error("[{}] {} {} failed: {}", traceId, method, path, detail, exception);
            return;
        }

        log.warn("[{}] {} {} -> {} {}", traceId, method, path, status.value(), detail);
    }

    private boolean hasResponseStatusAnnotation(Exception exception) {
        return AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class) != null;
    }

    private ModelAndView buildErrorPage(HttpStatusCode status, String detail, HttpServletRequest request) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        String errorTitle = resolvedStatus == null ? "Fehler" : resolvedStatus.getReasonPhrase();
        String path = request.getRequestURI();
        String traceId = TraceIdSupport.resolve(request);

        ModelAndView modelAndView = new ModelAndView("pages/error", Map.of(
                "page",
                new ErrorPageModel(
                        "Textbuddy Fehler",
                        status.value(),
                        errorTitle,
                        detail,
                        path == null ? "" : path,
                        traceId
                )
        ));

        if (resolvedStatus != null) {
            modelAndView.setStatus(resolvedStatus);
        }

        return modelAndView;
    }
}
