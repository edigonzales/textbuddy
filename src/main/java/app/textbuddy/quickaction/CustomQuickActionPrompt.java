package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record CustomQuickActionPrompt(String userPrompt) {

    static final int MAX_PROMPT_LENGTH = 400;
    private static final Pattern DISALLOWED_CONTROL_CHARACTERS = Pattern.compile("[\\p{Cntrl}&&[^\\n\\r\\t]]");

    public enum ValidationResult {
        VALID,
        MISSING,
        TOO_LONG,
        INVALID_CHARACTERS
    }

    public CustomQuickActionPrompt {
        userPrompt = normalize(userPrompt);
    }

    public static ValidationResult validate(String value) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            return ValidationResult.MISSING;
        }

        if (normalized.length() > MAX_PROMPT_LENGTH) {
            return ValidationResult.TOO_LONG;
        }

        if (DISALLOWED_CONTROL_CHARACTERS.matcher(normalized).find()) {
            return ValidationResult.INVALID_CHARACTERS;
        }

        return ValidationResult.VALID;
    }

    public static Optional<CustomQuickActionPrompt> fromInput(String value) {
        if (validate(value) != ValidationResult.VALID) {
            return Optional.empty();
        }

        return Optional.of(new CustomQuickActionPrompt(value));
    }

    public String instruction() {
        return """
                Führe den folgenden Arbeitsauftrag auf den bereitgestellten Volltext aus.
                Arbeite nur am Inhalt des Volltexts und antworte ausschliesslich mit dem überarbeiteten Text.

                Arbeitsauftrag:
                %s
                """.formatted(userPrompt).stripTrailing();
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNullElse(value, "");
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n').trim();

        return Arrays.stream(normalized.split("\n", -1))
                .map(String::trim)
                .collect(Collectors.joining("\n"))
                .trim();
    }
}
