package app.textbuddy.integration.languagetool;

import java.util.List;

public record LanguageToolMatch(
        int offset,
        int length,
        String message,
        String shortMessage,
        String ruleId,
        List<String> replacements
) {
}
