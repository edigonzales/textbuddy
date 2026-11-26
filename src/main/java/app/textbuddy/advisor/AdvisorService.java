package app.textbuddy.advisor;

import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public final class AdvisorService {

    static final int RULE_BATCH_SIZE = 3;
    static final int MAX_RULES = 20;
    static final int MAX_DOCUMENTS = 5;
    static final int MAX_FINDINGS = 20;

    private final AdvisorCatalog advisorCatalog;
    private final TextbuddyLlmClient llmClient;

    public AdvisorService(AdvisorCatalog advisorCatalog, TextbuddyLlmClient llmClient) {
        this.advisorCatalog = advisorCatalog;
        this.llmClient = llmClient;
    }

    public void validate(AdvisorValidateRequest request, AdvisorValidationStreamHandler handler) {
        String text = Objects.requireNonNullElse(request.text(), "");
        Set<String> selectedDocuments = documentNames(request.docs());
        validateSelectedDocuments(selectedDocuments);
        if (text.isBlank() || selectedDocuments.isEmpty()) {
            handler.complete(false);
            return;
        }

        List<AdvisorRuleCheck> rules = advisorCatalog.documents().stream()
                .filter(document -> selectedDocuments.contains(document.name()))
                .flatMap(document -> document.rules().stream().map(rule -> new AdvisorRuleCheck(document, rule)))
                .toList();
        int total = Math.min(rules.size(), MAX_RULES);
        boolean incomplete = rules.size() > total;
        Map<FindingKey, AdvisorValidationEvent> emitted = new HashMap<>();

        for (int checked = 0; checked < total && !Thread.currentThread().isInterrupted();) {
            int end = Math.min(checked + RULE_BATCH_SIZE, total);
            List<AdvisorRuleCheck> batch = rules.subList(checked, end);
            for (AdvisorRuleMatch match : llmClient.validate(text, batch)) {
                AdvisorRuleCheck check = batch.stream()
                        .filter(candidate -> candidate.document().name().equals(match.documentName())
                                && candidate.rule().id().equals(match.ruleId()))
                        .findFirst().orElse(null);
                int start = check == null ? -1 : locate(text, check.rule(), match);
                if (start < 0) {
                    incomplete = true;
                    continue;
                }

                AdvisorValidationEvent event = validationEvent(text, check, match, start);
                FindingKey key = new FindingKey(check.document().name(), check.rule().id(), start, event.end());
                AdvisorValidationEvent previous = emitted.get(key);
                if (previous != null) {
                    incomplete |= !previous.equals(event);
                } else if (emitted.size() == MAX_FINDINGS) {
                    incomplete = true;
                } else {
                    emitted.put(key, event);
                    handler.validation(event);
                }
            }
            checked = end;
            handler.progress(new AdvisorProgressEvent(checked, total));
        }

        if (!Thread.currentThread().isInterrupted()) {
            handler.complete(incomplete);
        }
    }

    public static Duration maximumValidationDuration(Duration providerTimeout) {
        int batchCount = (MAX_RULES + RULE_BATCH_SIZE - 1) / RULE_BATCH_SIZE;
        return providerTimeout.multipliedBy(batchCount).plusSeconds(10);
    }

    private int locate(String text, AdvisorRule rule, AdvisorRuleMatch match) {
        if (rule.matchTerms().stream().noneMatch(match.matchedText()::equalsIgnoreCase)) {
            return -1;
        }
        int context = uniqueOffset(text, match.excerpt());
        int relative = uniqueOffset(match.excerpt(), match.matchedText());
        return context < 0 || relative < 0 ? -1 : context + relative;
    }

    private int uniqueOffset(String text, String fragment) {
        int first = text.indexOf(fragment);
        return first >= 0 && text.indexOf(fragment, first + 1) < 0 ? first : -1;
    }

    private AdvisorValidationEvent validationEvent(String text, AdvisorRuleCheck check, AdvisorRuleMatch match, int start) {
        AdvisorDocument document = check.document();
        AdvisorRule rule = check.rule();
        int end = start + match.matchedText().length();
        String documentPath = UriUtils.encodePathSegment(document.name(), StandardCharsets.UTF_8);
        return new AdvisorValidationEvent(
                document.title(), rule.title(), rule.page(), match.message(), match.matchedText(),
                excerptAround(text, start, end),
                match.matchedText().equals(match.replacement()) ? null : match.replacement(),
                "/api/advisor/doc/" + documentPath + "#page=" + rule.page(), start, end
        );
    }

    private void validateSelectedDocuments(Set<String> selectedDocuments) {
        if (selectedDocuments.size() > MAX_DOCUMENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Höchstens " + MAX_DOCUMENTS + " Advisor-Dokumente sind erlaubt.");
        }
        for (String name : selectedDocuments) {
            if (advisorCatalog.find(name).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Advisor-Dokument ist unbekannt.");
            }
        }
    }

    private Set<String> documentNames(List<String> names) {
        if (names == null) return Set.of();
        return names.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String excerptAround(String text, int start, int end) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 40);
        if (from > 0 && Character.isLowSurrogate(text.charAt(from))) from--;
        if (to < text.length() && Character.isLowSurrogate(text.charAt(to))) to++;
        return (from > 0 ? "…" : "") + text.substring(from, to) + (to < text.length() ? "…" : "");
    }

    private record FindingKey(String documentName, String ruleId, int start, int end) {
    }
}
