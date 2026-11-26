package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SummarizePrompt {

    SENTENCE("sentence", "The summary should be exactly one sentence long."),
    THREE_SENTENCE("three_sentence", "The summary should be exactly three sentences long."),
    PARAGRAPH("paragraph", "The summary should be one paragraph long."),
    PAGE("page", "The summary should be up to one page long."),
    MANAGEMENT_SUMMARY("management_summary", """
            as a management summary.
            A management summary is a summary of the key points of a text for the management team.
            A management summary's length should be one paragraph up to one page long, depending on the length of the text.
            """.strip());

    private final String option;
    private final String requirement;

    SummarizePrompt(String option, String requirement) {
        this.option = option;
        this.requirement = requirement;
    }

    public String requirement() {
        return requirement;
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
