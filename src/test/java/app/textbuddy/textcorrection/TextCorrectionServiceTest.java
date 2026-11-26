package app.textbuddy.textcorrection;

import app.textbuddy.integration.languagetool.EmbeddedLanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolMatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TextCorrectionServiceTest {

    private final EmbeddedLanguageToolClient client = mock(EmbeddedLanguageToolClient.class);
    private final TextCorrectionService service = new TextCorrectionService(client);

    @Test
    void mapsLanguageToolMatchesToCorrectionBlocks() {
        when(client.check("This is teh text.", "en-US")).thenReturn(List.of(
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
        ));

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
        when(client.check("🌱x", "de")).thenReturn(List.of(
                new LanguageToolMatch(1, 0, "", "", "UNSAFE", List.of(" ")),
                new LanguageToolMatch(2, 0, "", "", "INSERT", List.of(" ")),
                new LanguageToolMatch(2, 1, "", "", "DELETE", List.of(""))
        ));

        assertThat(service.correct(new CorrectionRequest("🌱x", "de")).blocks())
                .extracting(CorrectionBlock::replacements)
                .containsExactly(List.of(" "), List.of(""));
    }

    @Test
    void defaultsLanguageToAutoWhenBlank() {
        service.correct(new CorrectionRequest("This is teh text.", "   "));

        verify(client).check("This is teh text.", "auto");
    }

    @Test
    void skipsBlankTextWithoutCallingLanguageTool() {
        CorrectionResponse response = service.correct(new CorrectionRequest("   ", "en-US"));

        verifyNoInteractions(client);
        assertThat(response.original()).isEqualTo("   ");
        assertThat(response.blocks()).isEmpty();
    }
}
