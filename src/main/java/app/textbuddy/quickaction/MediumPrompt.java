package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum MediumPrompt {

    EMAIL("email", "Formuliere den Text als klare E-Mail mit Betreff, Anrede und Abschluss."),
    OFFICIAL_LETTER(
            "official_letter",
            "Formuliere den Text als offizielles Schreiben mit formeller Anrede und Abschluss."
    ),
    PRESENTATION("presentation", "Formuliere den Text als Praesentationsentwurf mit klarer Folienstruktur."),
    REPORT("report", "Formuliere den Text als sachlichen Bericht mit strukturierter Einordnung.");

    private final String option;
    private final String instruction;

    MediumPrompt(String option, String instruction) {
        this.option = option;
        this.instruction = instruction;
    }

    public String option() {
        return option;
    }

    public String instruction() {
        return instruction;
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
