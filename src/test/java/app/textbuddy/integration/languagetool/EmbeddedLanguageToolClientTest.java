package app.textbuddy.integration.languagetool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedLanguageToolClientTest {

    private final EmbeddedLanguageToolClient client =
            new EmbeddedLanguageToolClient();

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
                    assertThat(match.length()).isNotNegative();
                    assertThat(match.ruleId()).isNotBlank();
                });
    }
    @Test
    void usesOriginalUtf16OffsetsWithLeadingWhitespaceAndUnicode() {
        String original = "  \n 🌱 This is\u00a0teh text.\n";
        int offset = original.indexOf("teh");

        assertThat(client.check(original, "en-US")).anySatisfy(match -> {
            assertThat(match.offset()).isEqualTo(offset);
            assertThat(original.substring(match.offset(), match.offset() + match.length())).isEqualTo("teh");
            assertThat(match.replacements()).contains("the");
        });
    }
}
