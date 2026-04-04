package app.textbuddy.advisor;

import java.util.List;

public record AdvisorRuleCheck(
        String documentName,
        String documentTitle,
        String referenceUrl,
        String ruleId,
        String ruleTitle,
        int page,
        String instructions,
        String message,
        String suggestion,
        List<String> matchTerms
) {
}
