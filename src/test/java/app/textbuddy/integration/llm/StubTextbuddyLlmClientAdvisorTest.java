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
    void advisorFixReturnsTheSelectedSuggestionsInOrder() {
        List<AdvisorFixFinding> findings = List.of(
                new AdvisorFixFinding("downloaden", "Bitte downloaden.", "Prüfen", "herunterladen"),
                new AdvisorFixFinding("per sofort", "Per sofort lesen.", "Prüfen", "ab sofort")
        );
        assertThat(client.fixAdvisor(findings)).containsExactly("herunterladen", "ab sofort");
    }
}
