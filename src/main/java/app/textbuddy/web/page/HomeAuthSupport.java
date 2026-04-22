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
import java.util.function.Function;

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

    public HomeAuthModel resolve(Authentication authentication, Function<String, String> messageResolver) {
        boolean authEnabled = properties.getAuth().isEnabled();
        boolean authenticated = isAuthenticated(authentication);
        Function<String, String> t = messageResolver == null ? Function.identity() : messageResolver;

        if (!authEnabled) {
            return new HomeAuthModel(
                    false,
                    false,
                    t.apply("auth.localMode.title"),
                    t.apply("auth.localMode.message"),
                    "",
                    ""
            );
        }

        if (authenticated) {
            String displayName = resolveDisplayName(authentication);

            return new HomeAuthModel(
                    true,
                    true,
                    t.apply("auth.oidcEnabled.title"),
                    t.apply("auth.oidcEnabled.authenticatedMessage"),
                    displayName,
                    resolveLoginUrl()
            );
        }

        return new HomeAuthModel(
                true,
                false,
                t.apply("auth.oidcEnabled.title"),
                t.apply("auth.oidcEnabled.unauthenticatedMessage"),
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
