package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CharacterSpeechPrompt {

    DIRECT_SPEECH("direct_speech"),
    INDIRECT_SPEECH("indirect_speech");

    private final String option;

    CharacterSpeechPrompt(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    public static Optional<CharacterSpeechPrompt> fromOption(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.option.equals(normalized))
                .findFirst();
    }
}
