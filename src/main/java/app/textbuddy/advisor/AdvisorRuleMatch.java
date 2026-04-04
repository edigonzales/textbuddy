package app.textbuddy.advisor;

public record AdvisorRuleMatch(
        String documentName,
        String ruleId,
        String matchedText,
        String excerpt,
        String message,
        String suggestion
) {
}
