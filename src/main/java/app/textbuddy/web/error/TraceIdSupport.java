package app.textbuddy.web.error;

import jakarta.servlet.http.HttpServletRequest;

public final class TraceIdSupport {

    public static final String TRACE_ID_ATTRIBUTE = TraceIdSupport.class.getName() + ".traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceIdSupport() {
    }

    public static String resolve(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        return traceId instanceof String value && !value.isBlank() ? value : "";
    }
}
