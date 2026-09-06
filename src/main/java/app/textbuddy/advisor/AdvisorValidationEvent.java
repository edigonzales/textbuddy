package app.textbuddy.advisor;

public record AdvisorValidationEvent(
        String stableKey,
        String documentName,
        String documentTitle,
        String ruleId,
        String ruleTitle,
        int page,
        String pageLabel,
        String message,
        String matchedText,
        String excerpt,
        String suggestion,
        String referenceUrl,
        int start,
        int end
) {
}
