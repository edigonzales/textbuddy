package app.textbuddy.web.page;

import app.textbuddy.config.TextbuddyProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HomeAuthSupport {

    private final TextbuddyProperties properties;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public HomeAuthSupport(
            TextbuddyProperties properties,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) {
        this.properties = properties;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    public HomeAuthModel resolve(Authentication authentication) {
        boolean authEnabled = properties.getAuth().isEnabled();
        boolean authenticated = isAuthenticated(authentication);

        if (!authEnabled) {
            return new HomeAuthModel(
                    false,
                    false,
                    "Lokaler Modus",
                    "OIDC ist deaktiviert. Alle vorhandenen Flows bleiben fuer lokale Entwicklung direkt verfuegbar.",
                    "",
                    ""
            );
        }

        if (authenticated) {
            String displayName = resolveDisplayName(authentication);

            return new HomeAuthModel(
                    true,
                    true,
                    "OIDC aktiv",
                    "APIs sind abgesichert. Die Arbeitsflaeche ist angemeldet und produktionsnah konfiguriert.",
                    displayName,
                    resolveLoginUrl()
            );
        }

        return new HomeAuthModel(
                true,
                false,
                "OIDC aktiv",
                "APIs erfordern eine Anmeldung. Melde dich ueber OIDC an, um Import, Korrektur, Advisor und Rewrites zu nutzen.",
                "",
                resolveLoginUrl()
        );
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String resolveDisplayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2AuthenticatedPrincipal oauth2Principal) {
            String preferredUsername = oauth2Principal.getAttribute("preferred_username");

            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }

            String name = oauth2Principal.getAttribute("name");

            if (name != null && !name.isBlank()) {
                return name;
            }

            String email = oauth2Principal.getAttribute("email");

            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        return authentication.getName();
    }

    private String resolveLoginUrl() {
        ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();

        if (!(repository instanceof Iterable<?> iterable)) {
            return "/login";
        }

        List<String> registrationIds = new ArrayList<>();

        for (Object candidate : iterable) {
            if (candidate instanceof ClientRegistration registration) {
                registrationIds.add(registration.getRegistrationId());
            }
        }

        if (registrationIds.size() == 1) {
            return "/oauth2/authorization/" + registrationIds.getFirst();
        }

        return "/login";
    }
}
