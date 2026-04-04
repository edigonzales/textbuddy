package app.textbuddy.quickaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomQuickActionRequestValidatorTest {

    private final CustomQuickActionRequestValidator validator = new CustomQuickActionRequestValidator();

    @Test
    void reportsAMissingPrompt() {
        assertThat(validator.validate(new QuickActionStreamRequest("Projektstart ist morgen.", "de-DE", null, "   ")))
                .isEqualTo(CustomQuickActionPrompt.ValidationResult.MISSING);
    }

    @Test
    void reportsATooLongPrompt() {
        assertThat(validator.validate(
                new QuickActionStreamRequest("Projektstart ist morgen.", "de-DE", null, "x".repeat(401))
        )).isEqualTo(CustomQuickActionPrompt.ValidationResult.TOO_LONG);
    }

    @Test
    void reportsInvalidControlCharacters() {
        assertThat(validator.validate(
                new QuickActionStreamRequest("Projektstart ist morgen.", "de-DE", null, "Bitte\u0007 umschreiben.")
        )).isEqualTo(CustomQuickActionPrompt.ValidationResult.INVALID_CHARACTERS);
    }

    @Test
    void acceptsAValidPrompt() {
        assertThat(validator.validate(
                new QuickActionStreamRequest("Projektstart ist morgen.", "de-DE", null, "  Bitte als interne Ankuendigung umschreiben.  ")
        )).isEqualTo(CustomQuickActionPrompt.ValidationResult.VALID);
    }
}
