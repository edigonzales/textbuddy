package app.textbuddy.advisor;

import java.util.List;

public record AdvisorRule(
        String id,
        String title,
        int page,
        String instructions,
        String message,
        String suggestion,
        List<String> matchTerms
) {
}
