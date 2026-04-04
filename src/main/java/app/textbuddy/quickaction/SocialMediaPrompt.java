package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SocialMediaPrompt {

    BLUESKY("bluesky", "Formuliere den Text als kurzen, pointierten Bluesky-Post."),
    INSTAGRAM("instagram", "Formuliere den Text als Instagram-Caption mit Hook und Hashtags."),
    LINKEDIN("linkedin", "Formuliere den Text als professionellen LinkedIn-Post mit klarem Nutzen.");

    private final String option;
    private final String instruction;

    SocialMediaPrompt(String option, String instruction) {
        this.option = option;
        this.instruction = instruction;
    }

    public String option() {
        return option;
    }

    public String instruction() {
        return instruction;
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
