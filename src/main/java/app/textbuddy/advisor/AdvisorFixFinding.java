package app.textbuddy.advisor;

public record AdvisorFixFinding(
        String documentName,
        String documentTitle,
        String ruleId,
        String ruleTitle,
        String instructions,
        String canonicalSuggestion,
        int start,
        int end,
        String matchedText,
        String suggestion
) {
}
