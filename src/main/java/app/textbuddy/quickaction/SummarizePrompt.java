package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SummarizePrompt {

    SENTENCE("sentence"),
    THREE_SENTENCE("three_sentence"),
    PARAGRAPH("paragraph"),
    PAGE("page"),
    MANAGEMENT_SUMMARY("management_summary");

    private final String option;

    SummarizePrompt(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    public static Optional<SummarizePrompt> fromOption(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.option.equals(normalized))
                .findFirst();
    }
}
