package app.textbuddy.advisor;

import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
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
                    .map(ruleCheck -> ruleCheck.document().name() + "::" + ruleCheck.rule().id())
                    .toList());

            return ruleChecks.stream()
                    .filter(ruleCheck -> ruleCheck.rule().id().equals("rule-1") || ruleCheck.rule().id().equals("rule-4"))
                    .map(ruleCheck -> new AdvisorRuleMatch(
                            ruleCheck.document().name(),
                            ruleCheck.rule().id(),
                            ruleCheck.rule().id().equals("rule-1") ? "downloaden" : "per sofort",
                            ruleCheck.rule().matchTerms().getFirst(),
                            "Verständlicher formulieren.",
                            ruleCheck.rule().id().equals("rule-1") ? "herunterladen" : "ab sofort"
                    ))
                    .toList();
        });
        AdvisorService service = new AdvisorService(catalog, llmClient);
        RecordingHandler handler = new RecordingHandler();

        service.validate(
                new AdvisorValidateRequest("Bitte downloaden Sie das Formular per sofort.", List.of("doc-a", "doc-b")),
                handler
        );

        assertThat(requestedBatches).containsExactly(
                List.of("doc-a::rule-1", "doc-a::rule-2", "doc-b::rule-3"),
                List.of("doc-b::rule-4", "doc-b::rule-5")
        );
        assertThat(handler.validations)
                .extracting(AdvisorValidationEvent::start, AdvisorValidationEvent::end, AdvisorValidationEvent::replacement)
                .containsExactly(
                        tuple(6, 16, "herunterladen"),
                        tuple(34, 44, "ab sofort")
                );
        assertThat(handler.progress).containsExactly(
                new AdvisorProgressEvent(3, 5),
                new AdvisorProgressEvent(5, 5)
        );
        assertThat(handler.validations)
                .extracting(AdvisorValidationEvent::referenceUrl)
                .containsExactly(
                        "/api/advisor/doc/doc-a#page=3",
                        "/api/advisor/doc/doc-b#page=8"
                );
        assertThat(handler.completions).containsExactly(false);
    }

    @Test
    void validateCompletesWithoutCallingLlmWhenNoTextOrDocumentsArePresent() {
        List<List<String>> requestedBatches = new ArrayList<>();
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.validate(anyString(), anyList())).thenAnswer(invocation -> {
            List<AdvisorRuleCheck> ruleChecks = invocation.getArgument(1);
            requestedBatches.add(ruleChecks.stream().map(check -> check.rule().id()).toList());
            return List.of();
        });
        AdvisorService service = new AdvisorService(
                catalog(document("doc-a", "Dokument A", 1, List.of(rule("rule-1", 3, List.of("downloaden"))))),
                llmClient
        );
        RecordingHandler handler = new RecordingHandler();

        service.validate(new AdvisorValidateRequest("   ", List.of()), handler);

        assertThat(requestedBatches).isEmpty();
        assertThat(handler.validations).isEmpty();
        assertThat(handler.completions).containsExactly(false);
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 21})
    void reportsIncompleteOnlyWhenTheRuleLimitIsExceeded(int ruleCount) {
        List<AdvisorRule> rules = new ArrayList<>();

        for (int index = 1; index <= ruleCount; index += 1) {
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
        AdvisorService service = new AdvisorService(catalog, llmClient);
        RecordingHandler handler = new RecordingHandler();

        service.validate(new AdvisorValidateRequest("Text", List.of("doc-a")), handler);

        assertThat(requestedBatchSizes).containsExactly(3, 3, 3, 3, 3, 3, 2);
        assertThat(handler.validations).isEmpty();
        assertThat(handler.completions).containsExactly(ruleCount > 20);
    }

    @Test
    void verifiesTheRepeatedTermUsingItsExactContextAndUtf16Offsets() {
        String text = "📄 Bitte downloaden.\nDann downloaden!";
        RecordingHandler handler = validateMatches(text, List.of(
                match("rule-1", "downloaden", "Dann downloaden!", "  erneut herunterladen\n")));

        assertThat(handler.validations).singleElement().satisfies(event -> {
            assertThat(event.start()).isEqualTo(text.lastIndexOf("downloaden"));
            assertThat(text.substring(event.start(), event.end())).isEqualTo("downloaden");
            assertThat(event.replacement()).isEqualTo("  erneut herunterladen\n");
        });
        assertThat(handler.completions).containsExactly(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"downloaden", "…downloaden!", "downloaden. Dann downloaden!", "📄"})
    void reportsIncompleteWhenAReportedContextDoesNotIdentifyOneOccurrence(String excerpt) {
        RecordingHandler handler = validateMatches("📄 downloaden. Dann downloaden!", List.of(
                match("rule-1", "downloaden", excerpt, "herunterladen")));

        assertThat(handler.validations).isEmpty();
        assertThat(handler.completions).containsExactly(true);
    }

    @Test
    void reportsIncompleteForAFindingOutsideItsBatchOrCanonicalTerms() {
        for (AdvisorRuleMatch finding : List.of(
                match("rule-4", "downloaden", "downloaden", "herunterladen"),
                match("unknown", "downloaden", "downloaden", "herunterladen"),
                match("rule-1", "Bitte", "Bitte", "Jetzt"))) {
            RecordingHandler handler = validateMatches("Bitte downloaden.", List.of(finding));
            assertThat(handler.validations).isEmpty();
            assertThat(handler.completions).containsExactly(true);
        }
    }

    @Test
    void deduplicatesIdenticalFindingsAndReportsConflictingReplacements() {
        AdvisorRuleMatch finding = match("rule-1", "downloaden", "downloaden", "herunterladen");
        RecordingHandler duplicate = validateMatches("Bitte downloaden.", List.of(finding, finding));
        assertThat(duplicate.validations).hasSize(1);
        assertThat(duplicate.completions).containsExactly(false);

        RecordingHandler conflict = validateMatches("Bitte downloaden.", List.of(finding,
                match("rule-1", "downloaden", "downloaden", "speichern")));
        assertThat(conflict.validations).hasSize(1);
        assertThat(conflict.completions).containsExactly(true);
    }

    @Test
    void preservesOverlappingFindingsFromDifferentRulesAndTheirReplacementSemantics() {
        RecordingHandler handler = validateMatches("Bitte downloaden.", List.of(
                match("rule-1", "downloaden", "downloaden", ""),
                match("rule-2", "downloaden", "downloaden", null),
                match("rule-3", "downloaden", "downloaden", "downloaden")));

        assertThat(handler.validations).extracting(AdvisorValidationEvent::replacement).containsExactly("", null, null);
        assertThat(handler.completions).containsExactly(false);
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 21})
    void reportsIncompleteOnlyWhenTheFindingLimitIsExceeded(int count) {
        List<AdvisorRuleMatch> matches = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            String sentence = "(%d) downloaden.".formatted(i);
            text.append(sentence).append(' ');
            matches.add(match("rule-1", "downloaden", sentence, "herunterladen"));
        }
        matches.add(matches.getFirst());
        RecordingHandler handler = validateMatches(text.toString(), matches);

        assertThat(handler.validations).hasSize(20);
        assertThat(handler.completions).containsExactly(count > 20);
    }

    @Test
    void validateRejectsMoreThanFiveSelectedDocuments() {
        AdvisorService service = new AdvisorService(mock(AdvisorCatalog.class), mock(TextbuddyLlmClient.class));

        assertThatThrownBy(() -> service.validate(
                new AdvisorValidateRequest("Text", List.of("a", "b", "c", "d", "e", "f")),
                new RecordingHandler()
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
    }

    private static RecordingHandler validateMatches(String text, List<AdvisorRuleMatch> matches) {
        AdvisorCatalog catalog = catalog(document("doc-a", "Dokument A", 1, List.of(
                rule("rule-1", 1, List.of("downloaden")), rule("rule-2", 2, List.of("downloaden")),
                rule("rule-3", 3, List.of("downloaden")), rule("rule-4", 4, List.of("downloaden"))
        )));
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.validate(anyString(), anyList())).thenReturn(matches).thenReturn(List.of());
        RecordingHandler handler = new RecordingHandler();
        new AdvisorService(catalog, llmClient).validate(new AdvisorValidateRequest(text, List.of("doc-a")), handler);
        return handler;
    }

    private static AdvisorRuleMatch match(String rule, String matchedText, String excerpt, String replacement) {
        return new AdvisorRuleMatch("doc-a", rule, matchedText, excerpt, "Bitte prüfen.", replacement);
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
        private final List<Boolean> completions = new ArrayList<>();

        @Override
        public void validation(AdvisorValidationEvent event) {
            validations.add(event);
        }

        @Override
        public void progress(AdvisorProgressEvent event) {
            progress.add(event);
        }

        @Override
        public void complete(boolean incomplete) {
            completions.add(incomplete);
        }
    }
}
