package app.textbuddy.quickaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomQuickActionPromptTest {

    @Test
    void normalizesWhitespaceAndBuildsTheInstruction() {
        assertThat(CustomQuickActionPrompt.fromInput("""
                  Formuliere den Text als interne Ankuendigung.
                    Nutze einen klaren Abschluss.
                """))
                .hasValueSatisfying(prompt -> {
                    assertThat(prompt.userPrompt()).isEqualTo("""
                            Formuliere den Text als interne Ankuendigung.
                            Nutze einen klaren Abschluss.
                            """.stripTrailing());
                    assertThat(prompt.instruction()).isEqualTo("""
                            Fuehre den folgenden Arbeitsauftrag auf den bereitgestellten Volltext aus.
                            Arbeite nur am Inhalt des Volltexts und antworte ausschliesslich mit dem ueberarbeiteten Text.

                            Arbeitsauftrag:
                            Formuliere den Text als interne Ankuendigung.
                            Nutze einen klaren Abschluss.
                            """.stripTrailing());
                });
    }
}
