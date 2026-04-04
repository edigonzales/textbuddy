package app.textbuddy.web.error;

public record ApiErrorResponse(
        String title,
        int status,
        String detail,
        String path,
        String traceId,
        String timestamp
) {
}
