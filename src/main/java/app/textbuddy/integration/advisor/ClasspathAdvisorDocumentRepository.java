package app.textbuddy.integration.advisor;

import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorDocumentFile;
import app.textbuddy.advisor.AdvisorRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ClasspathAdvisorDocumentRepository implements AdvisorDocumentRepository {

    private static final Comparator<AdvisorDocument> DOCUMENT_ORDER =
            Comparator.comparingInt(AdvisorDocument::order)
                    .thenComparing(AdvisorDocument::title);

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;
    private final String metadataLocationPattern;
    private final String documentLocationPattern;

    public ClasspathAdvisorDocumentRepository(
            ResourcePatternResolver resourcePatternResolver,
            ObjectMapper objectMapper
    ) {
        this(resourcePatternResolver, objectMapper, "classpath*:advisor/meta/*.json", "classpath:advisor/docs/%s.pdf");
    }

    public ClasspathAdvisorDocumentRepository(
            ResourcePatternResolver resourcePatternResolver,
            ObjectMapper objectMapper,
            String metadataLocationPattern,
            String documentLocationPattern
    ) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.objectMapper = objectMapper;
        this.metadataLocationPattern = metadataLocationPattern;
        this.documentLocationPattern = documentLocationPattern;
    }

    @Override
    public List<AdvisorDocument> findAll() {
        return loadDocuments().stream()
                .sorted(DOCUMENT_ORDER)
                .toList();
    }

    @Override
    public Optional<AdvisorDocumentFile> findDocument(String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }

        return loadDocuments().stream()
                .filter(document -> document.name().equals(name))
                .findFirst()
                .map(this::toDocumentFile);
    }

    private List<AdvisorDocument> loadDocuments() {
        try {
            Resource[] metadataResources = resourcePatternResolver.getResources(metadataLocationPattern);
            List<AdvisorDocument> documents = new ArrayList<>(metadataResources.length);

            for (Resource metadataResource : metadataResources) {
                MetadataFile metadata = readMetadata(metadataResource);
                List<AdvisorRule> rules = validateRules(metadata.rules(), metadataResource);
                Resource pdfResource = resolveDocumentResource(metadata.name());

                if (!pdfResource.exists() || !pdfResource.isReadable()) {
                    throw new IllegalStateException("Advisor PDF is missing or unreadable for document '" + metadata.name() + "'.");
                }

                documents.add(new AdvisorDocument(
                        metadata.order(),
                        metadata.name(),
                        metadata.title(),
                        metadata.summary(),
                        metadata.source(),
                        metadata.name() + ".pdf",
                        rules
                ));
            }

            return List.copyOf(documents);
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Failed to load advisor metadata resources.", exception);
        }
    }

    private MetadataFile readMetadata(Resource metadataResource) {
        try (InputStream inputStream = metadataResource.getInputStream()) {
            MetadataFile metadata = objectMapper.readValue(inputStream, MetadataFile.class);
            return validate(metadata, metadataResource);
        }
        catch (IOException exception) {
            throw new UncheckedIOException("Failed to read advisor metadata from " + describe(metadataResource) + ".", exception);
        }
    }

    private MetadataFile validate(MetadataFile metadata, Resource metadataResource) {
        if (metadata == null) {
            throw new IllegalStateException("Advisor metadata is empty in " + describe(metadataResource) + ".");
        }
        if (metadata.order() < 0) {
            throw new IllegalStateException("Advisor metadata order must be >= 0 in " + describe(metadataResource) + ".");
        }

        String name = requireText(metadata.name(), "name", metadataResource);
        String title = requireText(metadata.title(), "title", metadataResource);
        String summary = requireText(metadata.summary(), "summary", metadataResource);
        String source = requireText(metadata.source(), "source", metadataResource);
        return new MetadataFile(metadata.order(), name, title, summary, source, metadata.rules());
    }

    private List<AdvisorRule> validateRules(List<RuleFile> rules, Resource metadataResource) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException("Advisor metadata must define at least one rule in " + describe(metadataResource) + ".");
        }

        List<AdvisorRule> validatedRules = new ArrayList<>(rules.size());

        for (RuleFile rule : rules) {
            if (rule == null) {
                throw new IllegalStateException("Advisor metadata contains an empty rule in " + describe(metadataResource) + ".");
            }
            if (rule.page() <= 0) {
                throw new IllegalStateException("Advisor rule page must be > 0 in " + describe(metadataResource) + ".");
            }

            List<String> matchTerms = rule.matchTerms() == null
                    ? List.of()
                    : rule.matchTerms().stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();

            if (matchTerms.isEmpty()) {
                throw new IllegalStateException("Advisor rule matchTerms must not be empty in " + describe(metadataResource) + ".");
            }

            validatedRules.add(new AdvisorRule(
                    requireText(rule.id(), "rules.id", metadataResource),
                    requireText(rule.title(), "rules.title", metadataResource),
                    rule.page(),
                    requireText(rule.instructions(), "rules.instructions", metadataResource),
                    requireText(rule.message(), "rules.message", metadataResource),
                    requireText(rule.suggestion(), "rules.suggestion", metadataResource),
                    List.copyOf(matchTerms)
            ));
        }

        return List.copyOf(validatedRules);
    }

    private String requireText(String value, String fieldName, Resource metadataResource) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Advisor metadata field '" + fieldName + "' is missing in " + describe(metadataResource) + "."
            );
        }

        return value.trim();
    }

    private AdvisorDocumentFile toDocumentFile(AdvisorDocument document) {
        return new AdvisorDocumentFile(
                document.name(),
                document.pdfFileName(),
                resolveDocumentResource(document.name())
        );
    }

    private Resource resolveDocumentResource(String documentName) {
        return resourcePatternResolver.getResource(documentLocationPattern.formatted(documentName));
    }

    private String describe(Resource resource) {
        try {
            return resource.getURI().toString();
        }
        catch (IOException exception) {
            return resource.getDescription();
        }
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
