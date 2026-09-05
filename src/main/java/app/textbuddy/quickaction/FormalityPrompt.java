package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum FormalityPrompt {

    FORMAL("formal"),
    INFORMAL("informal");

    private final String option;

    FormalityPrompt(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    public static Optional<FormalityPrompt> fromOption(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.option.equals(normalized))
                .findFirst();
    }
}
