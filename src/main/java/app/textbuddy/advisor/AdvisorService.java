package app.textbuddy.advisor;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public final class AdvisorService {

    static final int DEFAULT_RULE_BATCH_SIZE = 3;
    static final int MAX_RULES = 20;
    static final int MAX_DOCUMENTS = 5;
    static final int MAX_FINDINGS = 20;

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);

    private final AdvisorCatalog advisorCatalog;
    private final TextbuddyLlmClient llmClient;
    private final int ruleBatchSize;

    @Autowired
    public AdvisorService(AdvisorCatalog advisorCatalog, TextbuddyLlmClient llmClient) {
        this(advisorCatalog, llmClient, DEFAULT_RULE_BATCH_SIZE);
    }

    AdvisorService(AdvisorCatalog advisorCatalog, TextbuddyLlmClient llmClient, int ruleBatchSize) {
        this.advisorCatalog = Objects.requireNonNull(advisorCatalog);
        this.llmClient = Objects.requireNonNull(llmClient);
        this.ruleBatchSize = Math.max(1, ruleBatchSize);
    }

    public void validate(AdvisorValidateRequest request, AdvisorValidationStreamHandler handler) {
        Objects.requireNonNull(handler);

        String text = request == null ? "" : Objects.requireNonNullElse(request.text(), "");
        Set<String> selectedDocuments = documentNames(request == null ? List.of() : request.docs());
        validateSelectedDocuments(selectedDocuments);

        if (text.isBlank() || selectedDocuments.isEmpty()) {
            handler.complete();
            return;
        }

        List<AdvisorRuleCheck> ruleChecks = loadRuleChecks(selectedDocuments).stream()
                .limit(MAX_RULES)
                .toList();
        if (ruleChecks.isEmpty()) {
            handler.complete();
            return;
        }

        Map<String, Integer> nextMatchOffsets = new HashMap<>();
        Set<String> emittedKeys = new HashSet<>();
        int checked = 0;

        for (List<AdvisorRuleCheck> batch : partitionRuleChecks(ruleChecks, ruleBatchSize)) {
            Map<String, AdvisorRuleCheck> checksByKey = batch.stream().collect(Collectors.toMap(
                    ruleCheck -> ruleKey(ruleCheck.documentName(), ruleCheck.ruleId()),
                    Function.identity()
            ));

            List<AdvisorRuleMatch> matches = Objects.requireNonNullElse(llmClient.validate(text, batch), List.of());
            for (AdvisorRuleMatch match : matches) {
                if (match == null || emittedKeys.size() >= MAX_FINDINGS) {
                    continue;
                }
                AdvisorRuleCheck ruleCheck = checksByKey.get(ruleKey(match.documentName(), match.ruleId()));
                locateEvent(text, ruleCheck, match, nextMatchOffsets).ifPresent(event -> {
                    if (emittedKeys.add(event.stableKey())) {
                        handler.validation(event);
                    } else if (ruleCheck != null) {
                        log.debug("Duplicate advisor finding discarded (document={}, rule={}).",
                                ruleCheck.documentName(), ruleCheck.ruleId());
                    }
                });
            }

            checked += batch.size();
            handler.progress(new AdvisorProgressEvent(checked, ruleChecks.size()));
        }

        handler.complete();
    }

    public AdvisorFixResponse fix(AdvisorFixRequest request) {
        String text = request == null ? "" : Objects.requireNonNullElse(request.text(), "");
        List<AdvisorFixRequest.Finding> requested = request == null || request.findings() == null
                ? List.of()
                : request.findings();

        if (text.isBlank()) {
            badRequest("Text ist erforderlich.");
        }
        if (requested.isEmpty()) {
            badRequest("Mindestens ein Advisor-Befund ist erforderlich.");
        }
        if (requested.size() > MAX_FINDINGS) {
            badRequest("Höchstens " + MAX_FINDINGS + " Advisor-Befunde sind erlaubt.");
        }

        List<AdvisorFixFinding> findings = new ArrayList<>(requested.size());
        Set<String> uniqueRanges = new HashSet<>();
        Set<String> uniqueDocuments = new HashSet<>();

        for (AdvisorFixRequest.Finding finding : requested) {
            AdvisorFixFinding resolved = resolveFinding(text, finding, uniqueRanges);
            findings.add(resolved);
            uniqueDocuments.add(resolved.documentName());
        }
        if (uniqueDocuments.size() > MAX_DOCUMENTS) {
            badRequest("Höchstens " + MAX_DOCUMENTS + " Advisor-Dokumente sind erlaubt.");
        }

        String corrected = Objects.requireNonNullElse(llmClient.fixAdvisor(text, List.copyOf(findings)), "");
        if (corrected.isBlank()) {
            throw new LlmProviderException("LLM-Provider lieferte keinen korrigierten Text.");
        }

        return new AdvisorFixResponse(preserveOuterWhitespace(text, corrected));
    }

    public static Duration maximumValidationDuration(Duration providerTimeout) {
        int batchCount = (MAX_RULES + DEFAULT_RULE_BATCH_SIZE - 1) / DEFAULT_RULE_BATCH_SIZE;
        return providerTimeout.multipliedBy(batchCount).plusSeconds(10);
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

    private java.util.Optional<AdvisorValidationEvent> locateEvent(
            String text,
            AdvisorRuleCheck ruleCheck,
            AdvisorRuleMatch match,
            Map<String, Integer> nextMatchOffsets
    ) {
        if (ruleCheck == null || match == null) {
            return java.util.Optional.empty();
        }
        String matchedText = Objects.requireNonNullElse(match.matchedText(), "");
        if (matchedText.isEmpty() || !matchesCanonicalTerm(matchedText, ruleCheck.matchTerms())) {
            log.debug("Unlocatable advisor finding discarded (document={}, rule={}).",
                    ruleCheck.documentName(), ruleCheck.ruleId());
            return java.util.Optional.empty();
        }

        String cursorKey = ruleKey(ruleCheck.documentName(), ruleCheck.ruleId()) + "::" + matchedText;
        int start = text.indexOf(matchedText, nextMatchOffsets.getOrDefault(cursorKey, 0));
        if (start < 0) {
            log.debug("Unlocatable advisor finding discarded (document={}, rule={}).",
                    ruleCheck.documentName(), ruleCheck.ruleId());
            return java.util.Optional.empty();
        }
        int end = start + matchedText.length();
        nextMatchOffsets.put(cursorKey, end);

        return java.util.Optional.of(new AdvisorValidationEvent(
                ruleKey(ruleCheck.documentName(), ruleCheck.ruleId()) + "::" + start + ":" + end,
                ruleCheck.documentName(),
                ruleCheck.documentTitle(),
                ruleCheck.ruleId(),
                ruleCheck.ruleTitle(),
                ruleCheck.page(),
                "Seite " + ruleCheck.page(),
                fallback(match.message(), ruleCheck.message()),
                matchedText,
                fallback(match.excerpt(), excerptAround(text, start, end)),
                fallback(match.suggestion(), ruleCheck.suggestion()),
                ruleCheck.referenceUrl(),
                start,
                end
        ));
    }

    private AdvisorFixFinding resolveFinding(
            String text,
            AdvisorFixRequest.Finding finding,
            Set<String> uniqueRanges
    ) {
        if (finding == null) {
            badRequest("Advisor-Befund ist ungültig.");
        }
        String documentName = normalized(finding.documentName());
        String ruleId = normalized(finding.ruleId());
        String suggestion = normalized(finding.suggestion());
        if (documentName.isBlank() || ruleId.isBlank() || suggestion.isBlank()) {
            badRequest("Dokument, Regel und Vorschlag sind erforderlich.");
        }
        if (finding.start() < 0 || finding.end() <= finding.start() || finding.end() > text.length()) {
            badRequest("Fundstelle liegt ausserhalb des Textes.");
        }

        AdvisorDocument document = advisorCatalog.find(documentName)
                .orElseThrow(() -> badRequestException("Advisor-Dokument ist unbekannt."));
        AdvisorRule rule = document.rules().stream()
                .filter(candidate -> candidate.id().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> badRequestException("Advisor-Regel ist unbekannt."));
        String matchedText = text.substring(finding.start(), finding.end());
        if (matchedText.isBlank() || !matchesCanonicalTerm(matchedText, rule.matchTerms())) {
            badRequest("Fundstelle stimmt nicht mit der ausgewählten Regel überein.");
        }
        String rangeKey = finding.start() + ":" + finding.end();
        if (!uniqueRanges.add(rangeKey)) {
            badRequest("Advisor-Fundstellen dürfen nicht doppelt vorkommen.");
        }

        return new AdvisorFixFinding(
                document.name(), document.title(), rule.id(), rule.title(), rule.instructions(),
                rule.suggestion(), finding.start(), finding.end(), matchedText, suggestion
        );
    }

    private boolean matchesCanonicalTerm(String matchedText, List<String> terms) {
        return terms.stream().anyMatch(term -> matchedText.equalsIgnoreCase(term));
    }

    private void validateSelectedDocuments(Set<String> selectedDocuments) {
        if (selectedDocuments.size() > MAX_DOCUMENTS) {
            badRequest("Höchstens " + MAX_DOCUMENTS + " Advisor-Dokumente sind erlaubt.");
        }
        for (String name : selectedDocuments) {
            if (advisorCatalog.find(name).isEmpty()) {
                badRequest("Advisor-Dokument ist unbekannt: " + name);
            }
        }
    }

    private List<AdvisorRuleCheck> loadRuleChecks(Set<String> selectedDocuments) {
        return advisorCatalog.documents().stream()
                .filter(document -> selectedDocuments.contains(document.name()))
                .flatMap(document -> document.rules().stream().map(rule -> toRuleCheck(document, rule)))
                .toList();
    }

    private AdvisorRuleCheck toRuleCheck(AdvisorDocument document, AdvisorRule rule) {
        return new AdvisorRuleCheck(
                document.name(), document.title(),
                "/api/advisor/doc/" + UriUtils.encodePathSegment(document.name(), StandardCharsets.UTF_8) + "#page=" + rule.page(),
                rule.id(), rule.title(), rule.page(), rule.instructions(), rule.message(), rule.suggestion(),
                List.copyOf(rule.matchTerms())
        );
    }

    private Set<String> documentNames(List<String> names) {
        if (names == null) {
            return Set.of();
        }
        return names.stream().map(this::normalized).filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String ruleKey(String documentName, String ruleId) {
        return normalized(documentName) + "::" + normalized(ruleId);
    }

    private String fallback(String preferred, String fallback) {
        String value = normalized(preferred);
        return value.isBlank() ? normalized(fallback) : value;
    }

    private String excerptAround(String text, int start, int end) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 40);
        return (from > 0 ? "…" : "") + text.substring(from, to) + (to < text.length() ? "…" : "");
    }

    private String preserveOuterWhitespace(String original, String corrected) {
        int leadingEnd = 0;
        while (leadingEnd < original.length() && Character.isWhitespace(original.charAt(leadingEnd))) {
            leadingEnd++;
        }
        int trailingStart = original.length();
        while (trailingStart > leadingEnd && Character.isWhitespace(original.charAt(trailingStart - 1))) {
            trailingStart--;
        }
        String leading = original.substring(0, leadingEnd);
        String trailing = original.substring(trailingStart);
        String value = corrected;
        if (!leading.isEmpty() && !value.startsWith(leading)) {
            value = leading + value.stripLeading();
        }
        if (!trailing.isEmpty() && !value.endsWith(trailing)) {
            value = value.stripTrailing() + trailing;
        }
        return value;
    }

    private String normalized(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private void badRequest(String message) {
        throw badRequestException(message);
    }

    private ResponseStatusException badRequestException(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
