package app.textbuddy.advisor;

public record AdvisorValidationEvent(
        String documentTitle,
        String ruleTitle,
        int page,
        String message,
        String matchedText,
        String excerpt,
        String replacement,
        String referenceUrl,
        int start,
        int end
) {
}
