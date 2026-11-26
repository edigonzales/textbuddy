package app.textbuddy.textcorrection;

import app.textbuddy.integration.languagetool.LanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TextCorrectionServiceTest {

    @Test
    void mapsLanguageToolMatchesToCorrectionBlocks() {
        LanguageToolClient client = (text, language) -> List.of(
                new LanguageToolMatch(
                        8,
                        3,
                        "Possible spelling mistake found.",
                        "Spelling",
                        "MORFOLOGIK_RULE_EN_US",
                        List.of("the", "the", "ten")
                ),
                new LanguageToolMatch(
                        -1,
                        2,
                        "Invalid",
                        "",
                        "INVALID",
                        List.of("ignored")
                )
        );
        TextCorrectionService service = new TextCorrectionService(client);

        CorrectionResponse response = service.correct(new CorrectionRequest("This is teh text.", "en-US"));

        assertThat(response.original()).isEqualTo("This is teh text.");
        assertThat(response.blocks()).containsExactly(
                new CorrectionBlock(
                        8,
                        3,
                        "Possible spelling mistake found.",
                        "Spelling",
                        "MORFOLOGIK_RULE_EN_US",
                        List.of("the", "ten")
                )
        );
    }

    @Test
    void preservesInsertionAndDeletionSuggestionsAtValidCharacterBoundaries() {
        LanguageToolClient client = (text, language) -> List.of(
                new LanguageToolMatch(1, 0, "", "", "UNSAFE", List.of(" ")),
                new LanguageToolMatch(2, 0, "", "", "INSERT", List.of(" ")),
                new LanguageToolMatch(2, 1, "", "", "DELETE", List.of(""))
        );
        var service = new TextCorrectionService(client);

        assertThat(service.correct(new CorrectionRequest("🌱x", "de")).blocks())
                .extracting(CorrectionBlock::replacements)
                .containsExactly(List.of(" "), List.of(""));
    }

    @Test
    void defaultsLanguageToAutoWhenBlank() {
        AtomicReference<String> capturedLanguage = new AtomicReference<>();
        LanguageToolClient client = (text, language) -> {
            capturedLanguage.set(language);
            return List.of();
        };
        TextCorrectionService service = new TextCorrectionService(client);

        service.correct(new CorrectionRequest("This is teh text.", "   "));

        assertThat(capturedLanguage.get()).isEqualTo("auto");
    }

    @Test
    void skipsBlankTextWithoutCallingLanguageTool() {
        AtomicBoolean called = new AtomicBoolean(false);
        LanguageToolClient client = (text, language) -> {
            called.set(true);
            return List.of();
        };
        TextCorrectionService service = new TextCorrectionService(client);

        CorrectionResponse response = service.correct(new CorrectionRequest("   ", "en-US"));

        assertThat(called).isFalse();
        assertThat(response.original()).isEqualTo("   ");
        assertThat(response.blocks()).isEmpty();
    }
}
