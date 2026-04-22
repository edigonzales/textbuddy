package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.advisor.AdvisorDocsResponseItem;
import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorDocumentFile;
import app.textbuddy.advisor.AdvisorRule;
import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.FORBIDDEN;

class AdvisorRoleAccessServiceTest {

    @Test
    void authDisabledShowsAllDocumentsAndAllowsRestrictedPdf() {
        AdvisorRoleAccessService service = createService(false);

        assertThat(service.listDocuments(new TestingAuthenticationToken("user", "pw", "ROLE_USER")))
                .extracting(AdvisorDocsResponseItem::name)
                .containsExactly("public-doc", "restricted-doc");

        assertThat(service.requireDocument("restricted-doc", new TestingAuthenticationToken("user", "pw", "ROLE_USER")))
                .isNotNull();
    }

    @Test
    void authEnabledFiltersRestrictedDocumentsWithoutRole() {
        AdvisorRoleAccessService service = createService(true);
        TestingAuthenticationToken user = new TestingAuthenticationToken("user", "pw", "ROLE_USER");

        assertThat(service.listDocuments(user))
                .extracting(AdvisorDocsResponseItem::name)
                .containsExactly("public-doc");

        assertThatThrownBy(() -> service.requireDocument("restricted-doc", user))
                .isInstanceOf(ResponseStatusException.class)
                .matches((exception) -> ((ResponseStatusException) exception).getStatusCode() == FORBIDDEN);
    }

    @Test
    void authEnabledAllowsRestrictedDocumentsWithMatchingRole() {
        AdvisorRoleAccessService service = createService(true);
        TestingAuthenticationToken advisor = new TestingAuthenticationToken(
                "advisor",
                "pw",
                "ROLE_USER",
                "ROLE_ADVISOR_INTERNAL"
        );

        assertThat(service.listDocuments(advisor))
                .extracting(AdvisorDocsResponseItem::name)
                .containsExactly("public-doc", "restricted-doc");

        assertThatCode(() -> service.assertValidationAccess(List.of("restricted-doc"), advisor))
                .doesNotThrowAnyException();
    }

    private AdvisorRoleAccessService createService(boolean authEnabled) {
        List<AdvisorDocument> documents = List.of(
                new AdvisorDocument(
                        1,
                        "public-doc",
                        "Public",
                        "Summary",
                        "Source",
                        List.of(),
                        "public-doc.pdf",
                        List.of(dummyRule())
                ),
                new AdvisorDocument(
                        2,
                        "restricted-doc",
                        "Restricted",
                        "Summary",
                        "Source",
                        List.of("ROLE_ADVISOR_INTERNAL"),
                        "restricted-doc.pdf",
                        List.of(dummyRule())
                )
        );

        AdvisorCatalogService catalogService = new AdvisorCatalogService() {
            @Override
            public List<AdvisorDocsResponseItem> listDocuments() {
                return List.of(
                        new AdvisorDocsResponseItem("public-doc", "Public", "Summary", "Source", "/api/advisor/doc/public-doc"),
                        new AdvisorDocsResponseItem(
                                "restricted-doc",
                                "Restricted",
                                "Summary",
                                "Source",
                                "/api/advisor/doc/restricted-doc"
                        )
                );
            }

            @Override
            public Optional<AdvisorDocumentFile> getDocument(String name) {
                return Optional.of(new AdvisorDocumentFile(
                        name,
                        name + ".pdf",
                        new ByteArrayResource(new byte[]{1, 2, 3})
                ));
            }
        };

        AdvisorDocumentRepository repository = new AdvisorDocumentRepository() {
            @Override
            public List<AdvisorDocument> findAll() {
                return documents;
            }

            @Override
            public Optional<AdvisorDocumentFile> findDocument(String name) {
                return Optional.empty();
            }
        };

        TextbuddyProperties properties = new TextbuddyProperties();
        properties.getAuth().setEnabled(authEnabled);
        return new AdvisorRoleAccessService(catalogService, repository, properties);
    }

    private AdvisorRule dummyRule() {
        return new AdvisorRule(
                "rule-id",
                "Rule",
                1,
                "Instruction",
                "Message",
                "Suggestion",
                List.of("term")
        );
    }
}
