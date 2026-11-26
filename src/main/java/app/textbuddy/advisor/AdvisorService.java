package app.textbuddy.advisor;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public final class AdvisorService {

    static final int RULE_BATCH_SIZE = 3;
    static final int MAX_RULES = 20;
    static final int MAX_DOCUMENTS = 5;
    static final int MAX_FINDINGS = 20;

    private static final Logger log = LoggerFactory.getLogger(AdvisorService.class);

    private final AdvisorCatalog advisorCatalog;
    private final TextbuddyLlmClient llmClient;

    public AdvisorService(AdvisorCatalog advisorCatalog, TextbuddyLlmClient llmClient) {
        this.advisorCatalog = advisorCatalog;
        this.llmClient = llmClient;
    }

    public void validate(AdvisorValidateRequest request, AdvisorValidationStreamHandler handler) {
        Objects.requireNonNull(handler);

        String text = request == null ? "" : Objects.requireNonNullElse(request.text(), "");
        Set<String> selectedDocuments = documentNames(request == null ? List.of() : request.docs());
        validateSelectedDocuments(selectedDocuments);

        if (text.isBlank() || selectedDocuments.isEmpty()) {
            handler.complete(false);
            return;
        }

        List<AdvisorRuleCheck> rules = loadRuleChecks(selectedDocuments);
        int total = Math.min(rules.size(), MAX_RULES);
        boolean truncated = rules.size() > total;
        Set<String> emitted = new HashSet<>();

        for (int checked = 0; checked < total && !Thread.currentThread().isInterrupted();) {
            int end = Math.min(checked + RULE_BATCH_SIZE, total);
            List<AdvisorRuleCheck> batch = rules.subList(checked, end);
            List<AdvisorRuleMatch> matches = llmClient.validate(text, batch);
            if (matches == null) {
                throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Befunde.");
            }
            for (AdvisorRuleMatch match : matches) {
                if (match == null) continue;
                AdvisorRuleCheck rule = batch.stream()
                        .filter(candidate -> candidate.documentName().equals(match.documentName())
                                && candidate.ruleId().equals(match.ruleId()))
                        .findFirst().orElse(null);
                AdvisorValidationEvent event = locateEvent(text, rule, match).orElse(null);
                if (event == null || emitted.contains(event.stableKey())) continue;
                if (emitted.size() == MAX_FINDINGS) {
                    truncated = true;
                    continue;
                }
                emitted.add(event.stableKey());
                handler.validation(event);
            }
            checked = end;
            handler.progress(new AdvisorProgressEvent(checked, total));
        }

        if (!Thread.currentThread().isInterrupted()) {
            handler.complete(truncated);
        }
    }

    public List<String> fix(AdvisorFixRequest request) {
        String text = request == null ? "" : Objects.requireNonNullElse(request.text(), "");
        List<AdvisorFixRequest.Finding> selected = request == null || request.findings() == null
                ? List.of()
                : request.findings();

        if (text.isBlank()) {
            badRequest("Text ist erforderlich.");
        }
        if (selected.isEmpty() || selected.size() > MAX_FINDINGS) {
            badRequest("Es sind 1 bis " + MAX_FINDINGS + " Advisor-Befunde erforderlich.");
        }

        List<AdvisorFixFinding> findings = new ArrayList<>(selected.size());
        Set<String> documents = new HashSet<>();
        int previousEnd = 0;
        for (AdvisorFixRequest.Finding finding : selected) {
            if (finding == null || finding.start() < previousEnd) {
                badRequest("Advisor-Fundstellen müssen geordnet sein und dürfen sich nicht überschneiden.");
            }
            findings.add(resolveFinding(text, finding));
            previousEnd = finding.end();
            documents.add(normalized(finding.documentName()));
        }
        if (documents.size() > MAX_DOCUMENTS) {
            badRequest("Höchstens " + MAX_DOCUMENTS + " Advisor-Dokumente sind erlaubt.");
        }

        List<String> replacements = llmClient.fixAdvisor(findings);
        if (replacements == null || replacements.size() != findings.size()
                || replacements.stream().anyMatch(Objects::isNull)) {
            throw new LlmProviderException("LLM-Provider lieferte keine gültigen Advisor-Korrekturen.");
        }
        return replacements;
    }

    public static Duration maximumValidationDuration(Duration providerTimeout) {
        int batchCount = (MAX_RULES + RULE_BATCH_SIZE - 1) / RULE_BATCH_SIZE;
        return providerTimeout.multipliedBy(batchCount).plusSeconds(10);
    }

    private Optional<AdvisorValidationEvent> locateEvent(String text, AdvisorRuleCheck rule, AdvisorRuleMatch match) {
        if (rule == null || !matchesCanonicalTerm(match.matchedText(), rule.matchTerms())) {
            return Optional.empty();
        }
        String matchedText = match.matchedText();
        int start = uniqueOffset(text, matchedText);
        if (start < 0 && match.excerpt() != null && !match.excerpt().isBlank()) {
            int context = uniqueOffset(text, match.excerpt());
            int relative = uniqueOffset(match.excerpt(), matchedText);
            if (context >= 0 && relative >= 0) start = context + relative;
        }
        if (start < 0) {
            log.debug("Unlocatable advisor finding discarded (document={}, rule={}).", rule.documentName(), rule.ruleId());
            return Optional.empty();
        }
        int end = start + matchedText.length();
        return Optional.of(new AdvisorValidationEvent(
                rule.documentName() + "::" + rule.ruleId() + "::" + start + ":" + end,
                rule.documentName(),
                rule.documentTitle(),
                rule.ruleId(),
                rule.ruleTitle(),
                rule.page(),
                fallback(match.message(), rule.message()),
                matchedText,
                excerptAround(text, start, end),
                fallback(match.suggestion(), rule.suggestion()),
                rule.referenceUrl(),
                start,
                end
        ));
    }

    private int uniqueOffset(String text, String fragment) {
        int first = text.indexOf(fragment);
        return first >= 0 && text.indexOf(fragment, first + 1) < 0 ? first : -1;
    }

    private AdvisorFixFinding resolveFinding(String text, AdvisorFixRequest.Finding finding) {
        String documentName = normalized(finding.documentName());
        String ruleId = normalized(finding.ruleId());
        String suggestion = finding.suggestion();
        if (documentName.isBlank() || ruleId.isBlank() || suggestion == null) {
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
        if (!matchedText.equals(finding.matchedText()) || !matchesCanonicalTerm(matchedText, rule.matchTerms())) {
            badRequest("Fundstelle stimmt nicht mit dem ausgewählten Befund überein.");
        }
        return new AdvisorFixFinding(matchedText, excerptAround(text, finding.start(), finding.end()),
                rule.instructions(), suggestion);
    }

    private boolean matchesCanonicalTerm(String matchedText, List<String> terms) {
        return matchedText != null && !matchedText.isBlank()
                && terms.stream().anyMatch(matchedText::equalsIgnoreCase);
    }

    private void validateSelectedDocuments(Set<String> selectedDocuments) {
        if (selectedDocuments.size() > MAX_DOCUMENTS) {
            badRequest("Höchstens " + MAX_DOCUMENTS + " Advisor-Dokumente sind erlaubt.");
        }
        for (String name : selectedDocuments) {
            if (advisorCatalog.find(name).isEmpty()) {
                badRequest("Advisor-Dokument ist unbekannt.");
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

    private String fallback(String preferred, String fallback) {
        String value = normalized(preferred);
        return value.isBlank() ? normalized(fallback) : value;
    }

    private String excerptAround(String text, int start, int end) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 40);
        return (from > 0 ? "…" : "") + text.substring(from, to) + (to < text.length() ? "…" : "");
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
