package app.textbuddy.web.error;

import app.textbuddy.config.WebI18nConfiguration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;
import java.util.Map;

@ControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private final ApiErrorResponseFactory errorResponseFactory;
    private final MessageSource messageSource;

    public GlobalErrorHandler(ApiErrorResponseFactory errorResponseFactory, MessageSource messageSource) {
        this.errorResponseFactory = errorResponseFactory;
        this.messageSource = messageSource;
    }

    @ExceptionHandler(Exception.class)
    public Object handle(Exception exception, HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        HttpStatusCode status = resolveStatus(exception);
        String detail = resolveDetail(exception, status, locale);

        logException(exception, request, status, detail);

        if (isApiRequest(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(errorResponseFactory.create(status, detail, request));
        }

        return buildErrorPage(status, detail, request, locale);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.startsWith("/api/");
    }

    private HttpStatusCode resolveStatus(Exception exception) {
        if (exception instanceof MaxUploadSizeExceededException) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }

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

    private String resolveDetail(Exception exception, HttpStatusCode status, Locale locale) {
        if (exception instanceof MaxUploadSizeExceededException) {
            return message("error.detail.fileTooLargeUpload", locale);
        }

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
            return defaultMessage(status, locale);
        }

        if (!status.is5xxServerError() && exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return defaultMessage(status, locale);
    }

    private String defaultMessage(HttpStatusCode status, Locale locale) {
        return switch (status.value()) {
            case 400 -> message("error.detail.400", locale);
            case 401 -> message("error.detail.401", locale);
            case 403 -> message("error.detail.403", locale);
            case 413 -> message("error.detail.413", locale);
            case 404 -> message("error.detail.404", locale);
            case 405 -> message("error.detail.405", locale);
            case 502 -> message("error.detail.502", locale);
            default -> message("error.detail.default", locale);
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

    private ModelAndView buildErrorPage(
            HttpStatusCode status,
            String detail,
            HttpServletRequest request,
            Locale locale
    ) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        String errorTitle = resolvedStatus == null
                ? message("error.titleFallback", locale)
                : resolvedStatus.getReasonPhrase();
        String path = request.getRequestURI();
        String traceId = TraceIdSupport.resolve(request);

        ModelAndView modelAndView = new ModelAndView("pages/error", Map.of(
                "page",
                new ErrorPageModel(
                        message("error.page.title", locale),
                        status.value(),
                        errorTitle,
                        detail,
                        path == null ? "" : path,
                        traceId,
                        locale == null ? "de" : locale.toLanguageTag(),
                        message("layout.skipLink", locale),
                        message("error.page.eyebrow", locale),
                        message("error.page.pathLabel", locale),
                        message("error.page.traceLabel", locale),
                        message("error.page.backHome", locale)
                )
        ));

        if (resolvedStatus != null) {
            modelAndView.setStatus(resolvedStatus);
        }

        return modelAndView;
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String requestedLanguage = request.getParameter(WebI18nConfiguration.UI_LOCALE_PARAMETER);
        String acceptLanguage = request.getHeader("Accept-Language");
        Cookie[] cookies = request.getCookies();
        boolean hasLocaleCookie = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (WebI18nConfiguration.UI_LOCALE_COOKIE.equals(cookie.getName())) {
                    hasLocaleCookie = true;
                    break;
                }
            }
        }

        if ((requestedLanguage == null || requestedLanguage.isBlank())
                && (acceptLanguage == null || acceptLanguage.isBlank())
                && !hasLocaleCookie) {
            return Locale.GERMAN;
        }

        Locale locale = RequestContextUtils.getLocale(request);
        return locale == null ? Locale.GERMAN : locale;
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale == null ? Locale.GERMAN : locale);
    }
}
