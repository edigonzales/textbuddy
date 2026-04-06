package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CharacterSpeechPrompt {

    DIRECT_SPEECH("direct_speech", "Formuliere den Text als direkte Rede mit wörtlichen Zitaten."),
    INDIRECT_SPEECH("indirect_speech", "Formuliere den Text als indirekte Rede mit berichteter Aussage.");

    private final String option;
    private final String instruction;

    CharacterSpeechPrompt(String option, String instruction) {
        this.option = option;
        this.instruction = instruction;
    }

    public String option() {
        return option;
    }

    public String instruction() {
        return instruction;
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
