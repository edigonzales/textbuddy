package app.textbuddy.advisor;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.llm.AdvisorValidationLlmClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAdvisorValidationServiceTest {

    @Test
    void validateSplitsSelectedRulesIntoSmallBatchesAndStreamsMatches() {
        AdvisorDocumentRepository repository = repository(
                document("doc-a", "Dokument A", 1, List.of(
                        rule("rule-1", 3, List.of("downloaden")),
                        rule("rule-2", 4, List.of("meeting"))
                )),
                document("doc-b", "Dokument B", 2, List.of(
                        rule("rule-3", 7, List.of("email")),
                        rule("rule-4", 8, List.of("per sofort")),
                        rule("rule-5", 10, List.of("beiliegend"))
                )),
                document("doc-c", "Dokument C", 3, List.of(
                        rule("rule-6", 12, List.of("buerger"))
                ))
        );
        List<List<String>> requestedBatches = new ArrayList<>();
        AdvisorValidationLlmClient llmClient = (text, ruleChecks) -> {
            requestedBatches.add(ruleChecks.stream()
                    .map(ruleCheck -> ruleCheck.documentName() + "::" + ruleCheck.ruleId())
                    .toList());

            return ruleChecks.stream()
                    .filter(ruleCheck -> ruleCheck.ruleId().equals("rule-1") || ruleCheck.ruleId().equals("rule-4"))
                    .map(ruleCheck -> new AdvisorRuleMatch(
                            ruleCheck.documentName(),
                            ruleCheck.ruleId(),
                            ruleCheck.ruleId().equals("rule-1") ? "downloaden" : "per sofort",
                            "Gefundener Auszug",
                            null,
                            null
                    ))
                    .toList();
        };
        DefaultAdvisorValidationService service = new DefaultAdvisorValidationService(repository, llmClient, 2);
        RecordingHandler handler = new RecordingHandler();

        service.validate(
                new AdvisorValidateRequest("Bitte downloaden Sie das Formular per sofort.", List.of("doc-a", "doc-b")),
                handler
        );

        assertThat(requestedBatches).containsExactly(
                List.of("doc-a::rule-1", "doc-a::rule-2"),
                List.of("doc-b::rule-3", "doc-b::rule-4"),
                List.of("doc-b::rule-5")
        );
        assertThat(handler.validations)
                .extracting(AdvisorValidationEvent::stableKey)
                .containsExactly(
                        "doc-a::rule-1::downloaden",
                        "doc-b::rule-4::per-sofort"
                );
        assertThat(handler.validations)
                .extracting(AdvisorValidationEvent::referenceUrl)
                .containsExactly(
                        "/api/advisor/doc/doc-a#page=3",
                        "/api/advisor/doc/doc-b#page=8"
                );
        assertThat(handler.completeCount).isEqualTo(1);
        assertThat(handler.errors).isEmpty();
    }

    @Test
    void validateCompletesWithoutCallingLlmWhenNoTextOrDocumentsArePresent() {
        List<List<String>> requestedBatches = new ArrayList<>();
        AdvisorValidationLlmClient llmClient = (text, ruleChecks) -> {
            requestedBatches.add(ruleChecks.stream().map(AdvisorRuleCheck::ruleId).toList());
            return List.of();
        };
        DefaultAdvisorValidationService service = new DefaultAdvisorValidationService(
                repository(document("doc-a", "Dokument A", 1, List.of(rule("rule-1", 3, List.of("downloaden"))))),
                llmClient,
                2
        );
        RecordingHandler handler = new RecordingHandler();

        service.validate(new AdvisorValidateRequest("   ", List.of()), handler);

        assertThat(requestedBatches).isEmpty();
        assertThat(handler.validations).isEmpty();
        assertThat(handler.completeCount).isEqualTo(1);
        assertThat(handler.errors).isEmpty();
    }

    @Test
    void validateCapsTheAdvisorRulesAtTwentyEntries() {
        List<AdvisorRule> rules = new ArrayList<>();

        for (int index = 1; index <= 25; index += 1) {
            rules.add(rule("rule-" + index, index, List.of("term-" + index)));
        }

        AdvisorDocumentRepository repository = repository(document("doc-a", "Dokument A", 1, rules));
        List<Integer> requestedBatchSizes = new ArrayList<>();
        AdvisorValidationLlmClient llmClient = (text, ruleChecks) -> {
            requestedBatchSizes.add(ruleChecks.size());
            return List.of();
        };
        DefaultAdvisorValidationService service = new DefaultAdvisorValidationService(repository, llmClient, 3);
        RecordingHandler handler = new RecordingHandler();

        service.validate(new AdvisorValidateRequest("Text", List.of("doc-a")), handler);

        assertThat(requestedBatchSizes).containsExactly(3, 3, 3, 3, 3, 3, 2);
        assertThat(handler.completeCount).isEqualTo(1);
    }

    private static AdvisorDocumentRepository repository(AdvisorDocument... documents) {
        List<AdvisorDocument> values = List.of(documents);

        return new AdvisorDocumentRepository() {
            @Override
            public List<AdvisorDocument> findAll() {
                return values;
            }

            @Override
            public Optional<AdvisorDocumentFile> findDocument(String name) {
                return Optional.empty();
            }
        };
    }

    private static AdvisorDocument document(String name, String title, int order, List<AdvisorRule> rules) {
        return new AdvisorDocument(
                order,
                name,
                title,
                "Zusammenfassung",
                "Quelle",
                name + ".pdf",
                rules
        );
    }

    private static AdvisorRule rule(String id, int page, List<String> matchTerms) {
        return new AdvisorRule(
                id,
                "Regel " + id,
                page,
                "Pruefanweisung",
                "Hinweis fuer " + id,
                "Empfehlung fuer " + id,
                matchTerms
        );
    }

    private static final class RecordingHandler implements AdvisorValidationStreamHandler {

        private final List<AdvisorValidationEvent> validations = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private int completeCount;

        @Override
        public void validation(AdvisorValidationEvent event) {
            validations.add(event);
        }

        @Override
        public void complete() {
            completeCount += 1;
        }

        @Override
        public void error(String message) {
            errors.add(message);
        }
    }
}
