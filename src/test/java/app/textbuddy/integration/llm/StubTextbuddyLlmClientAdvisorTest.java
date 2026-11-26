package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorRule;
import app.textbuddy.advisor.AdvisorRuleCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StubTextbuddyLlmClientAdvisorTest {

    private final StubTextbuddyLlmClient client = new StubTextbuddyLlmClient();

    @Test
    void advisorTermsUseWordBoundariesAndDoNotFlagZuhanden() {
        AdvisorRuleCheck rule = rule("z.hd.");

        assertThat(client.validate("Zuhanden von Frau Meier", List.of(rule))).isEmpty();
        assertThat(client.validate("Brief z.Hd. Frau Meier", List.of(rule)))
                .singleElement().satisfies(match -> {
                    assertThat(match.matchedText()).isEqualTo("z.Hd.");
                    assertThat(match.replacement()).isEqualTo("zuhanden von");
                });
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Downloaden; danach Downloaden. | 0",
            "📄 Dies ist der lange einleitende Satz vor dem Befund. Downloaden; danach Downloaden. | 55",
            "downloaden undownloaden | 0",
            "undownloaden downloaden | 13",
            "Downloaden; danach Downloaden; danach Downloaden. | 19"
    })
    void repeatedTermUsesExactContextAndSkipsPartialWords(String text, int expectedStart) {
        AdvisorRuleCheck rule = rule("Downloaden");

        assertThat(client.validate("Zum undownloaden.", List.of(rule))).isEmpty();
        assertThat(client.validate(text, List.of(rule))).singleElement().satisfies(match -> {
            assertThat(match.matchedText()).isEqualToIgnoringCase("downloaden");
            assertThat(text).contains(match.excerpt());
            assertThat(text.indexOf(match.excerpt())).isEqualTo(text.lastIndexOf(match.excerpt()));
            assertThat(match.excerpt().indexOf(match.matchedText()))
                    .isEqualTo(match.excerpt().lastIndexOf(match.matchedText()));
            assertThat(text.indexOf(match.excerpt()) + match.excerpt().indexOf(match.matchedText()))
                    .isEqualTo(expectedStart);
            assertThat(match.replacement()).isEqualTo("herunterladen");
        });
    }

    private static AdvisorRuleCheck rule(String term) {
        AdvisorRule rule = new AdvisorRule("rule", "Regeltitel", 1, "Prüfen", "Ausschreiben",
                "Allgemeiner Hinweis", List.of(term));
        return new AdvisorRuleCheck(new AdvisorDocument(1, "doc", "Dokument", "Zusammenfassung", "Quelle", List.of(rule)),
                rule);
    }
}
