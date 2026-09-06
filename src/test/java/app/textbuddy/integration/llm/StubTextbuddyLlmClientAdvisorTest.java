package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
import app.textbuddy.advisor.AdvisorRuleCheck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StubTextbuddyLlmClientAdvisorTest {

    private final StubTextbuddyLlmClient client = new StubTextbuddyLlmClient();

    @Test
    void advisorTermsUseWordBoundariesAndDoNotFlagZuhanden() {
        AdvisorRuleCheck rule = new AdvisorRuleCheck("doc", "Dokument", "/doc", "zhd-ausschreiben",
                "Adressierung", 1, "Prüfen", "Ausschreiben", "zuhanden von", List.of("z.hd."));

        assertThat(client.validate("Zuhanden von Frau Meier", List.of(rule))).isEmpty();
        assertThat(client.validate("Brief z.Hd. Frau Meier", List.of(rule)))
                .singleElement().satisfies(match -> {
                    assertThat(match.matchedText()).isEqualTo("z.Hd.");
                    assertThat(match.suggestion()).isEqualTo("zuhanden von");
                });
    }

    @Test
    void advisorFixAppliesSelectedSuggestionsFromRightToLeft() {
        String text = "Bitte downloaden und per sofort lesen.";
        List<AdvisorFixFinding> findings = List.of(
                finding("downloaden-statt-herunterladen", 6, 16, "downloaden", "herunterladen"),
                finding("per-sofort-vermeiden", 21, 31, "per sofort", "ab sofort")
        );

        assertThat(client.fixAdvisor(text, findings)).isEqualTo("Bitte herunterladen und ab sofort lesen.");
    }

    private AdvisorFixFinding finding(String id, int start, int end, String matched, String suggestion) {
        return new AdvisorFixFinding("doc", "Dokument", id, "Regel", "Prüfen", suggestion,
                start, end, matched, suggestion);
    }
}
