package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SocialMediaPrompt {

    BLUESKY("bluesky"),
    INSTAGRAM("instagram"),
    LINKEDIN("linkedin");

    private final String option;

    SocialMediaPrompt(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    public static Optional<SocialMediaPrompt> fromOption(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.option.equals(normalized))
                .findFirst();
    }
}
