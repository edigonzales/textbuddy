package app.textbuddy.web.error;

public record ErrorPageModel(
        String title,
        int statusCode,
        String errorTitle,
        String message,
        String path,
        String traceId,
        String lang,
        String skipLinkLabel,
        String eyebrow,
        String pathLabel,
        String traceLabel,
        String backHomeLabel
) {
}
