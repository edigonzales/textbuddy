package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum FormalityPrompt {

    FORMAL("formal", "Formuliere den Text formeller und professioneller."),
    INFORMAL("informal", "Formuliere den Text lockerer und nahbarer.");

    private final String option;
    private final String instruction;

    FormalityPrompt(String option, String instruction) {
        this.option = option;
        this.instruction = instruction;
    }

    public String option() {
        return option;
    }

    public String instruction() {
        return instruction;
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
