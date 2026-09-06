package app.textbuddy.advisor;

import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvisorServiceTest {

    @Test
    void streamBudgetCoversAllProviderBatchesAndMargin() {
        assertThat(AdvisorService.maximumValidationDuration(Duration.ofSeconds(30)))
                .isEqualTo(Duration.ofSeconds(220));
    }

    @Test
    void validateSplitsSelectedRulesIntoSmallBatchesAndStreamsMatches() {
        AdvisorCatalog catalog = catalog(
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
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.validate(anyString(), anyList())).thenAnswer(invocation -> {
            List<AdvisorRuleCheck> ruleChecks = invocation.getArgument(1);
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
        });
        AdvisorService service = new AdvisorService(catalog, llmClient, 2);
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
                        "doc-a::rule-1::6:16",
                        "doc-b::rule-4::34:44"
                );
        assertThat(handler.progress).containsExactly(
                new AdvisorProgressEvent(2, 5),
                new AdvisorProgressEvent(4, 5),
                new AdvisorProgressEvent(5, 5)
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
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.validate(anyString(), anyList())).thenAnswer(invocation -> {
            List<AdvisorRuleCheck> ruleChecks = invocation.getArgument(1);
            requestedBatches.add(ruleChecks.stream().map(AdvisorRuleCheck::ruleId).toList());
            return List.of();
        });
        AdvisorService service = new AdvisorService(
                catalog(document("doc-a", "Dokument A", 1, List.of(rule("rule-1", 3, List.of("downloaden"))))),
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

        AdvisorCatalog catalog = catalog(document("doc-a", "Dokument A", 1, rules));
        List<Integer> requestedBatchSizes = new ArrayList<>();
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.validate(anyString(), anyList())).thenAnswer(invocation -> {
            List<AdvisorRuleCheck> ruleChecks = invocation.getArgument(1);
            requestedBatchSizes.add(ruleChecks.size());
            return List.of();
        });
        AdvisorService service = new AdvisorService(catalog, llmClient, 3);
        RecordingHandler handler = new RecordingHandler();

        service.validate(new AdvisorValidateRequest("Text", List.of("doc-a")), handler);

        assertThat(requestedBatchSizes).containsExactly(3, 3, 3, 3, 3, 3, 2);
        assertThat(handler.completeCount).isEqualTo(1);
    }

    @Test
    void validateRejectsMoreThanFiveSelectedDocuments() {
        AdvisorService service = new AdvisorService(mock(AdvisorCatalog.class), mock(TextbuddyLlmClient.class), 3);

        assertThatThrownBy(() -> service.validate(
                new AdvisorValidateRequest("Text", List.of("a", "b", "c", "d", "e", "f")),
                new RecordingHandler()
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
    }

    private static AdvisorCatalog catalog(AdvisorDocument... documents) {
        List<AdvisorDocument> values = List.of(documents);
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.documents()).thenReturn(values);
        for (AdvisorDocument document : values) {
            when(catalog.find(document.name())).thenReturn(java.util.Optional.of(document));
        }
        return catalog;
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
        private final List<AdvisorProgressEvent> progress = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private int completeCount;

        @Override
        public void validation(AdvisorValidationEvent event) {
            validations.add(event);
        }

        @Override
        public void progress(AdvisorProgressEvent event) {
            progress.add(event);
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
