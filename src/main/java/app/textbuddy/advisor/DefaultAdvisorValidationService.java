package app.textbuddy.advisor;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.llm.AdvisorValidationLlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public final class DefaultAdvisorValidationService implements AdvisorValidationService {

    static final int DEFAULT_RULE_BATCH_SIZE = 2;

    private static final String ERROR_MESSAGE = "Advisor-Pruefung konnte nicht abgeschlossen werden.";

    private final AdvisorDocumentRepository advisorDocumentRepository;
    private final AdvisorValidationLlmClient advisorValidationLlmClient;
    private final int ruleBatchSize;

    @Autowired
    public DefaultAdvisorValidationService(
            AdvisorDocumentRepository advisorDocumentRepository,
            AdvisorValidationLlmClient advisorValidationLlmClient
    ) {
        this(advisorDocumentRepository, advisorValidationLlmClient, DEFAULT_RULE_BATCH_SIZE);
    }

    DefaultAdvisorValidationService(
            AdvisorDocumentRepository advisorDocumentRepository,
            AdvisorValidationLlmClient advisorValidationLlmClient,
            int ruleBatchSize
    ) {
        this.advisorDocumentRepository = Objects.requireNonNull(advisorDocumentRepository);
        this.advisorValidationLlmClient = Objects.requireNonNull(advisorValidationLlmClient);
        this.ruleBatchSize = Math.max(1, ruleBatchSize);
    }

    @Override
    public void validate(AdvisorValidateRequest request, AdvisorValidationStreamHandler handler) {
        Objects.requireNonNull(handler);

        String text = normalize(request == null ? null : request.text());
        Set<String> selectedDocuments = normalizeDocumentNames(request == null ? List.of() : request.docs());

        if (text.isBlank() || selectedDocuments.isEmpty()) {
            handler.complete();
            return;
        }

        try {
            List<AdvisorRuleCheck> ruleChecks = loadRuleChecks(selectedDocuments);

            if (ruleChecks.isEmpty()) {
                handler.complete();
                return;
            }

            for (List<AdvisorRuleCheck> batch : partitionRuleChecks(ruleChecks, ruleBatchSize)) {
                Map<String, AdvisorRuleCheck> ruleChecksByKey = batch.stream()
                        .collect(Collectors.toMap(
                                ruleCheck -> ruleKey(ruleCheck.documentName(), ruleCheck.ruleId()),
                                Function.identity()
                        ));

                for (AdvisorRuleMatch match : advisorValidationLlmClient.validate(text, batch)) {
                    AdvisorRuleCheck ruleCheck = ruleChecksByKey.get(ruleKey(match.documentName(), match.ruleId()));

                    if (ruleCheck != null) {
                        handler.validation(toEvent(ruleCheck, match));
                    }
                }
            }

            handler.complete();
        } catch (RuntimeException exception) {
            handler.error(ERROR_MESSAGE);
        }
    }

    List<List<AdvisorRuleCheck>> partitionRuleChecks(List<AdvisorRuleCheck> ruleChecks, int batchSize) {
        if (ruleChecks == null || ruleChecks.isEmpty()) {
            return List.of();
        }

        int normalizedBatchSize = Math.max(1, batchSize);
        List<List<AdvisorRuleCheck>> batches = new ArrayList<>();

        for (int index = 0; index < ruleChecks.size(); index += normalizedBatchSize) {
            batches.add(List.copyOf(ruleChecks.subList(index, Math.min(ruleChecks.size(), index + normalizedBatchSize))));
        }

        return List.copyOf(batches);
    }

    private List<AdvisorRuleCheck> loadRuleChecks(Set<String> selectedDocuments) {
        return advisorDocumentRepository.findAll().stream()
                .filter(document -> selectedDocuments.contains(document.name()))
                .flatMap(document -> document.rules().stream().map(rule -> toRuleCheck(document, rule)))
                .toList();
    }

    private AdvisorRuleCheck toRuleCheck(AdvisorDocument document, AdvisorRule rule) {
        return new AdvisorRuleCheck(
                document.name(),
                document.title(),
                "/api/advisor/doc/" + UriUtils.encodePathSegment(document.name(), StandardCharsets.UTF_8) + "#page=" + rule.page(),
                rule.id(),
                rule.title(),
                rule.page(),
                rule.instructions(),
                rule.message(),
                rule.suggestion(),
                List.copyOf(rule.matchTerms())
        );
    }

    private AdvisorValidationEvent toEvent(AdvisorRuleCheck ruleCheck, AdvisorRuleMatch match) {
        String matchedText = normalize(match.matchedText());
        String message = fallback(match.message(), ruleCheck.message());
        String suggestion = fallback(match.suggestion(), ruleCheck.suggestion());
        String excerpt = fallback(match.excerpt(), matchedText);

        return new AdvisorValidationEvent(
                stableKey(ruleCheck.documentName(), ruleCheck.ruleId(), matchedText),
                ruleCheck.documentName(),
                ruleCheck.documentTitle(),
                ruleCheck.ruleId(),
                ruleCheck.ruleTitle(),
                ruleCheck.page(),
                "Seite " + ruleCheck.page(),
                message,
                matchedText,
                excerpt,
                suggestion,
                ruleCheck.referenceUrl()
        );
    }

    private Set<String> normalizeDocumentNames(List<String> documentNames) {
        if (documentNames == null || documentNames.isEmpty()) {
            return Set.of();
        }

        return documentNames.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String stableKey(String documentName, String ruleId, String matchedText) {
        String key = ruleKey(documentName, ruleId);

        if (matchedText.isBlank()) {
            return key;
        }

        return key + "::" + stableSegment(matchedText);
    }

    private String stableSegment(String value) {
        String normalized = normalize(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");

        return normalized.replaceAll("(^-+|-+$)", "");
    }

    private String ruleKey(String documentName, String ruleId) {
        return normalize(documentName) + "::" + normalize(ruleId);
    }

    private String fallback(String preferred, String fallback) {
        String normalizedPreferred = normalize(preferred);

        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }

        return normalize(fallback);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
