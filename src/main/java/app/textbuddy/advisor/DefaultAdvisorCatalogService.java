package app.textbuddy.advisor;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class DefaultAdvisorCatalogService implements AdvisorCatalogService {

    private final AdvisorDocumentRepository advisorDocumentRepository;

    public DefaultAdvisorCatalogService(AdvisorDocumentRepository advisorDocumentRepository) {
        this.advisorDocumentRepository = advisorDocumentRepository;
    }

    @Override
    public List<AdvisorDocsResponseItem> listDocuments() {
        return advisorDocumentRepository.findAll().stream()
                .map(this::toResponseItem)
                .toList();
    }

    @Override
    public Optional<AdvisorDocumentFile> getDocument(String name) {
        return advisorDocumentRepository.findDocument(name);
    }

    private AdvisorDocsResponseItem toResponseItem(AdvisorDocument document) {
        return new AdvisorDocsResponseItem(
                document.name(),
                document.title(),
                document.summary(),
                document.source(),
                "/api/advisor/doc/" + UriUtils.encodePathSegment(document.name(), StandardCharsets.UTF_8)
        );
    }
}
