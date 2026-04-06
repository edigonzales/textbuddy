package app.textbuddy.integration.languagetool;

import app.textbuddy.config.LanguageToolProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedLanguageToolClientTest {

    private final EmbeddedLanguageToolClient client =
            new EmbeddedLanguageToolClient(new LanguageToolProperties());

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            auto|This is teh text.
            de-CH|Das ist teh Text.
            fr|Bonjour teh monde.
            it|Ciao teh mondo.
            en-US|This is teh text.
            en-GB|This is teh text.
            """)
    void checksTextWithEmbeddedLanguageToolAcrossConfiguredLanguages(String language, String text) {
        assertThat(client.check(text, language))
                .isNotEmpty()
                .allSatisfy(match -> {
                    assertThat(match.length()).isPositive();
                    assertThat(match.ruleId()).isNotBlank();
                });
    }
}
