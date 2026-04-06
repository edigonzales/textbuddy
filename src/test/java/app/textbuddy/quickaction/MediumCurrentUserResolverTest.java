package app.textbuddy.quickaction;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MediumCurrentUserResolverTest {

    private final MediumCurrentUserResolver resolver = new MediumCurrentUserResolver();

    @Test
    void resolvesNamedOauth2Attributes() {
        DefaultOAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal(
                Map.of(
                        "given_name", "Ada",
                        "family_name", "Lovelace",
                        "email", "ada@example.org",
                        "name", "Ada Lovelace"
                ),
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, "n/a", "ROLE_USER");
        authentication.setAuthenticated(true);

        MediumCurrentUser currentUser = resolver.resolve(authentication);

        assertThat(currentUser.givenName()).isEqualTo("Ada");
        assertThat(currentUser.familyName()).isEqualTo("Lovelace");
        assertThat(currentUser.email()).isEqualTo("ada@example.org");
    }

    @Test
    void returnsPlaceholdersWithoutAuthentication() {
        assertThat(resolver.resolve(null)).isEqualTo(MediumCurrentUser.placeholder());
    }
}
