package app.textbuddy.quickaction;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

@Component
public final class MediumCurrentUserResolver {

    public MediumCurrentUser resolve(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return MediumCurrentUser.placeholder();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2AuthenticatedPrincipal oauth2Principal) {
            String displayName = firstNonBlank(
                    oauth2Principal.getAttribute("name"),
                    oauth2Principal.getAttribute("preferred_username"),
                    authentication.getName()
            );

            return new MediumCurrentUser(
                    firstNonBlank(oauth2Principal.getAttribute("given_name"), firstToken(displayName)),
                    firstNonBlank(oauth2Principal.getAttribute("family_name"), remainingTokens(displayName)),
                    firstNonBlank(oauth2Principal.getAttribute("email"), emailLike(displayName))
            );
        }

        String name = authentication.getName();

        return new MediumCurrentUser(
                firstToken(name),
                remainingTokens(name),
                emailLike(name)
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return "";
    }

    private String firstToken(String value) {
        String normalized = firstNonBlank(value);

        if (normalized.isBlank()) {
            return "";
        }

        int separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(0, separator).trim();
    }

    private String remainingTokens(String value) {
        String normalized = firstNonBlank(value);

        if (normalized.isBlank()) {
            return "";
        }

        int separator = normalized.indexOf(' ');
        return separator < 0 ? "" : normalized.substring(separator + 1).trim();
    }

    private String emailLike(String value) {
        String normalized = firstNonBlank(value);
        return normalized.contains("@") ? normalized : "";
    }
}
