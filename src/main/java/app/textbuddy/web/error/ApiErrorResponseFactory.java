package app.textbuddy.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApiErrorResponseFactory {

    public ApiErrorResponse create(HttpStatusCode status, String detail, HttpServletRequest request) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());

        return new ApiErrorResponse(
                resolvedStatus == null ? "Fehler" : resolvedStatus.getReasonPhrase(),
                status.value(),
                detail,
                request.getRequestURI(),
                TraceIdSupport.resolve(request),
                Instant.now().toString()
        );
    }
}
