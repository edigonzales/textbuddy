package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.advisor.AdvisorDocsResponseItem;
import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorDocumentFile;
import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class AdvisorRoleAccessService {

    private final AdvisorCatalogService advisorCatalogService;
    private final AdvisorDocumentRepository advisorDocumentRepository;
    private final TextbuddyProperties textbuddyProperties;

    public AdvisorRoleAccessService(
            AdvisorCatalogService advisorCatalogService,
            AdvisorDocumentRepository advisorDocumentRepository,
            TextbuddyProperties textbuddyProperties
    ) {
        this.advisorCatalogService = advisorCatalogService;
        this.advisorDocumentRepository = advisorDocumentRepository;
        this.textbuddyProperties = textbuddyProperties;
    }

    public List<AdvisorDocsResponseItem> listDocuments(Authentication authentication) {
        if (!isRoleChecksEnabled()) {
            return advisorCatalogService.listDocuments();
        }

        Set<String> roles = resolveRoles(authentication);
        Set<String> visibleDocumentNames = advisorDocumentRepository.findAll().stream()
                .filter((document) -> hasAccess(document, roles))
                .map(AdvisorDocument::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return advisorCatalogService.listDocuments().stream()
                .filter((item) -> visibleDocumentNames.contains(item.name()))
                .toList();
    }

    public AdvisorDocumentFile requireDocument(String name, Authentication authentication) {
        Map<String, AdvisorDocument> documentsByName = documentsByName();
        AdvisorDocument document = documentsByName.get(normalize(name));

        if (document == null) {
            throw new ResponseStatusException(NOT_FOUND, "Advisor-Dokument wurde nicht gefunden: " + normalize(name));
        }

        if (!hasAccess(document, resolveRoles(authentication))) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Zugriff auf dieses Advisor-Dokument ist nicht erlaubt."
            );
        }

        return advisorCatalogService.getDocument(document.name())
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Advisor-Dokument wurde nicht gefunden: " + normalize(name)
                ));
    }

    public void assertValidationAccess(List<String> selectedDocumentNames, Authentication authentication) {
        if (!isRoleChecksEnabled()) {
            return;
        }

        Set<String> selectedNames = normalizeDocumentNames(selectedDocumentNames);

        if (selectedNames.isEmpty()) {
            return;
        }

        Set<String> roles = resolveRoles(authentication);
        Map<String, AdvisorDocument> documentsByName = documentsByName();

        for (String selectedName : selectedNames) {
            AdvisorDocument selectedDocument = documentsByName.get(selectedName);

            if (selectedDocument != null && !hasAccess(selectedDocument, roles)) {
                throw new ResponseStatusException(
                        FORBIDDEN,
                        "Zugriff auf mindestens ein ausgewähltes Advisor-Dokument ist nicht erlaubt."
                );
            }
        }
    }

    private Map<String, AdvisorDocument> documentsByName() {
        Map<String, AdvisorDocument> documentsByName = new LinkedHashMap<>();

        for (AdvisorDocument document : advisorDocumentRepository.findAll()) {
            documentsByName.put(document.name(), document);
        }

        return documentsByName;
    }

    private boolean hasAccess(AdvisorDocument document, Set<String> roles) {
        if (!isRoleChecksEnabled()) {
            return true;
        }

        List<String> allowedRoles = document.allowedRoles();

        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true;
        }

        return allowedRoles.stream().anyMatch(roles::contains);
    }

    private Set<String> resolveRoles(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter((value) -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private Set<String> normalizeDocumentNames(List<String> documentNames) {
        if (documentNames == null || documentNames.isEmpty()) {
            return Set.of();
        }

        return documentNames.stream()
                .map(this::normalize)
                .filter((value) -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isRoleChecksEnabled() {
        return textbuddyProperties.getAuth().isEnabled();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
