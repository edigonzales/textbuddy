package app.textbuddy.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsagePseudonymizerTest {

    private final UsagePseudonymizer pseudonymizer = new UsagePseudonymizer();

    @Test
    void pseudonymizesStableAndSaltedIdentifiers() {
        String first = pseudonymizer.pseudonymize("salt-a", "demo@example.org");
        String second = pseudonymizer.pseudonymize("salt-a", "demo@example.org");
        String differentSalt = pseudonymizer.pseudonymize("salt-b", "demo@example.org");

        assertThat(first).startsWith("u-");
        assertThat(second).isEqualTo(first);
        assertThat(differentSalt).isNotEqualTo(first);
        assertThat(first).doesNotContain("demo@example.org");
    }

    @Test
    void returnsAnonymousForMissingIdentifier() {
        assertThat(pseudonymizer.pseudonymize("salt-a", " ")).isEqualTo("anonymous");
    }
}
