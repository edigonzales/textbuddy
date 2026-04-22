package app.textbuddy.observability;

import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.web.error.TraceIdSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

public class ApiUsageLoggingInterceptor implements AsyncHandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiUsageLoggingInterceptor.class);
    private static final String START_NANO_ATTRIBUTE = ApiUsageLoggingInterceptor.class.getName() + ".startNanos";

    private final TextbuddyProperties textbuddyProperties;
    private final UsagePseudonymizer pseudonymizer;

    public ApiUsageLoggingInterceptor(
            TextbuddyProperties textbuddyProperties,
            UsagePseudonymizer pseudonymizer
    ) {
        this.textbuddyProperties = textbuddyProperties;
        this.pseudonymizer = pseudonymizer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isEnabled()) {
            return true;
        }

        request.setAttribute(START_NANO_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (!isEnabled()) {
            return;
        }

        long durationMillis = resolveDurationMillis(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = isAuthenticated(authentication);
        String actor = pseudonymizer.pseudonymize(
                textbuddyProperties.getObservability().getPseudonymSalt(),
                authenticated ? authentication.getName() : ""
        );

        log.info(
                "usage event=api_call traceId={} method={} path={} status={} durationMs={} actor={} authenticated={}",
                TraceIdSupport.resolve(request),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMillis,
                actor,
                authenticated
        );
    }

    private long resolveDurationMillis(HttpServletRequest request) {
        Object startedAt = request.getAttribute(START_NANO_ATTRIBUTE);

        if (!(startedAt instanceof Long startNanos)) {
            return 0L;
        }

        long durationNanos = Math.max(0L, System.nanoTime() - startNanos);
        return durationNanos / 1_000_000L;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isEnabled() {
        return textbuddyProperties.getObservability().isUsageLoggingEnabled();
    }
}
