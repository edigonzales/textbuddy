package app.textbuddy.advisor;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public final class AdvisorCatalog {

    private static final String METADATA_PATTERN = "classpath*:advisor/meta/*.json";
    private static final String DOCUMENT_PATTERN = "classpath:advisor/docs/%s.pdf";
    private static final Comparator<AdvisorDocument> DOCUMENT_ORDER = Comparator
            .comparingInt(AdvisorDocument::order)
            .thenComparing(AdvisorDocument::title);

    private final ResourcePatternResolver resources;
    private final List<AdvisorDocument> documents;

    public AdvisorCatalog(ResourcePatternResolver resources, ObjectMapper objectMapper) {
        this.resources = resources;
        this.documents = loadDocuments(objectMapper);
    }

    public List<AdvisorDocument> documents() {
        return documents;
    }

    public List<AdvisorDocsResponseItem> listDocuments() {
        return documents.stream()
                .map(document -> new AdvisorDocsResponseItem(
                        document.name(),
                        document.title(),
                        document.summary(),
                        document.source(),
                        "/api/advisor/doc/" + UriUtils.encodePathSegment(document.name(), StandardCharsets.UTF_8),
                        document.rules().size()
                ))
                .toList();
    }

    public Optional<AdvisorDocument> find(String name) {
        String normalized = name == null ? "" : name.trim();
        return documents.stream().filter(document -> document.name().equals(normalized)).findFirst();
    }

    public Optional<AdvisorDocumentFile> findDocument(String name) {
        return find(name).map(document -> new AdvisorDocumentFile(
                document.name() + ".pdf",
                resolveDocumentResource(document.name())
        ));
    }

    private List<AdvisorDocument> loadDocuments(ObjectMapper objectMapper) {
        try {
            Resource[] metadataResources = resources.getResources(METADATA_PATTERN);
            List<AdvisorDocument> loaded = new ArrayList<>(metadataResources.length);
            Set<String> documentNames = new HashSet<>();

            for (Resource metadataResource : metadataResources) {
                AdvisorDocument metadata;
                try (InputStream input = metadataResource.getInputStream()) {
                    metadata = objectMapper.readValue(input, AdvisorDocument.class);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Advisor-Metadaten konnten nicht gelesen werden: "
                            + metadataResource.getDescription(), exception);
                }
                if (metadata == null || metadata.order() < 0) {
                    throw new IllegalStateException("Ungültige Advisor-Metadaten in " + metadataResource.getDescription() + ".");
                }

                String name = requireText(metadata.name(), "name", metadataResource);
                if (!documentNames.add(name)) {
                    throw new IllegalStateException("Doppelte Advisor-Dokument-ID: " + name);
                }

                Resource pdf = resolveDocumentResource(name);
                if (!pdf.exists() || !pdf.isReadable()) {
                    throw new IllegalStateException("Advisor-PDF fehlt für Dokument '" + name + "'.");
                }

                loaded.add(new AdvisorDocument(
                        metadata.order(),
                        name,
                        requireText(metadata.title(), "title", metadataResource),
                        requireText(metadata.summary(), "summary", metadataResource),
                        requireText(metadata.source(), "source", metadataResource),
                        validateRules(metadata.rules(), metadataResource)
                ));
            }

            return loaded.stream().sorted(DOCUMENT_ORDER).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Advisor-Metadaten konnten nicht geladen werden.", exception);
        }
    }

    private List<AdvisorRule> validateRules(List<AdvisorRule> rules, Resource resource) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException("Advisor-Dokument benötigt mindestens eine Regel: " + resource.getDescription());
        }

        List<AdvisorRule> validated = new ArrayList<>(rules.size());
        Set<String> ids = new HashSet<>();

        for (AdvisorRule rule : rules) {
            if (rule == null || rule.page() <= 0) {
                throw new IllegalStateException("Ungültige Advisor-Regel in " + resource.getDescription());
            }

            String id = requireText(rule.id(), "rules.id", resource);
            if (!ids.add(id)) {
                throw new IllegalStateException("Doppelte Advisor-Regel-ID '" + id + "' in " + resource.getDescription());
            }

            List<String> matchTerms = rule.matchTerms() == null
                    ? List.of()
                    : rule.matchTerms().stream().map(String::trim).filter(StringUtils::hasText).toList();
            if (matchTerms.isEmpty()) {
                throw new IllegalStateException("Advisor-Regel benötigt matchTerms in " + resource.getDescription());
            }

            validated.add(new AdvisorRule(
                    id,
                    requireText(rule.title(), "rules.title", resource),
                    rule.page(),
                    requireText(rule.instructions(), "rules.instructions", resource),
                    requireText(rule.message(), "rules.message", resource),
                    requireText(rule.suggestion(), "rules.suggestion", resource),
                    matchTerms
            ));
        }

        return List.copyOf(validated);
    }

    private String requireText(String value, String field, Resource resource) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Advisor-Feld '" + field + "' fehlt in " + resource.getDescription());
        }
        return value.trim();
    }

    private Resource resolveDocumentResource(String name) {
        return resources.getResource(DOCUMENT_PATTERN.formatted(name));
    }
}
