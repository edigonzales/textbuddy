package app.textbuddy.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<String, AdvisorDocument> documentsByName;

    public AdvisorCatalog(ResourcePatternResolver resources, ObjectMapper objectMapper) {
        this.resources = resources;
        this.documents = loadDocuments(objectMapper);
        Map<String, AdvisorDocument> byName = new LinkedHashMap<>();
        documents.forEach(document -> byName.put(document.name(), document));
        this.documentsByName = Map.copyOf(byName);
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
                        "/api/advisor/doc/" + UriUtils.encodePathSegment(document.name(), StandardCharsets.UTF_8)
                ))
                .toList();
    }

    public Optional<AdvisorDocumentFile> findDocument(String name) {
        AdvisorDocument document = documentsByName.get(normalize(name));

        if (document == null) {
            return Optional.empty();
        }

        return Optional.of(new AdvisorDocumentFile(
                document.name(),
                document.pdfFileName(),
                resolveDocumentResource(document.name())
        ));
    }

    private List<AdvisorDocument> loadDocuments(ObjectMapper objectMapper) {
        try {
            Resource[] metadataResources = resources.getResources(METADATA_PATTERN);
            List<AdvisorDocument> loaded = new ArrayList<>(metadataResources.length);
            Set<String> documentNames = new HashSet<>();

            for (Resource metadataResource : metadataResources) {
                MetadataFile metadata = readMetadata(metadataResource, objectMapper);

                if (!documentNames.add(metadata.name())) {
                    throw new IllegalStateException("Doppelte Advisor-Dokument-ID: " + metadata.name());
                }

                Resource pdf = resolveDocumentResource(metadata.name());
                if (!pdf.exists() || !pdf.isReadable()) {
                    throw new IllegalStateException("Advisor-PDF fehlt für Dokument '" + metadata.name() + "'.");
                }

                loaded.add(new AdvisorDocument(
                        metadata.order(),
                        metadata.name(),
                        metadata.title(),
                        metadata.summary(),
                        metadata.source(),
                        metadata.name() + ".pdf",
                        validateRules(metadata.rules(), metadataResource)
                ));
            }

            return loaded.stream().sorted(DOCUMENT_ORDER).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Advisor-Metadaten konnten nicht geladen werden.", exception);
        }
    }

    private MetadataFile readMetadata(Resource resource, ObjectMapper objectMapper) {
        try (InputStream input = resource.getInputStream()) {
            MetadataFile metadata = objectMapper.readValue(input, MetadataFile.class);

            if (metadata == null || metadata.order() < 0) {
                throw new IllegalStateException("Ungültige Advisor-Metadaten in " + describe(resource) + ".");
            }

            return new MetadataFile(
                    metadata.order(),
                    requireText(metadata.name(), "name", resource),
                    requireText(metadata.title(), "title", resource),
                    requireText(metadata.summary(), "summary", resource),
                    requireText(metadata.source(), "source", resource),
                    metadata.rules()
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Advisor-Metadaten konnten nicht gelesen werden: " + describe(resource), exception);
        }
    }

    private List<AdvisorRule> validateRules(List<RuleFile> rules, Resource resource) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException("Advisor-Dokument benötigt mindestens eine Regel: " + describe(resource));
        }

        List<AdvisorRule> validated = new ArrayList<>(rules.size());
        Set<String> ids = new HashSet<>();

        for (RuleFile rule : rules) {
            if (rule == null || rule.page() <= 0) {
                throw new IllegalStateException("Ungültige Advisor-Regel in " + describe(resource));
            }

            String id = requireText(rule.id(), "rules.id", resource);
            if (!ids.add(id)) {
                throw new IllegalStateException("Doppelte Advisor-Regel-ID '" + id + "' in " + describe(resource));
            }

            List<String> matchTerms = rule.matchTerms() == null
                    ? List.of()
                    : rule.matchTerms().stream().map(String::trim).filter(StringUtils::hasText).toList();
            if (matchTerms.isEmpty()) {
                throw new IllegalStateException("Advisor-Regel benötigt matchTerms in " + describe(resource));
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
            throw new IllegalStateException("Advisor-Feld '" + field + "' fehlt in " + describe(resource));
        }
        return value.trim();
    }

    private Resource resolveDocumentResource(String name) {
        return resources.getResource(DOCUMENT_PATTERN.formatted(name));
    }

    private String describe(Resource resource) {
        return resource.getDescription();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record MetadataFile(
            int order,
            String name,
            String title,
            String summary,
            String source,
            List<RuleFile> rules
    ) {
    }

    private record RuleFile(
            String id,
            String title,
            int page,
            String instructions,
            String message,
            String suggestion,
            List<String> matchTerms
    ) {
    }
}
