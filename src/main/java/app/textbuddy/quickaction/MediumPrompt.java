package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MediumPrompt {

    EMAIL("email"),
    OFFICIAL_LETTER("official_letter"),
    PRESENTATION("presentation"),
    REPORT("report");

    private final String option;

    MediumPrompt(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    public static Optional<MediumPrompt> fromOption(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.option.equals(normalized))
                .findFirst();
    }
}
